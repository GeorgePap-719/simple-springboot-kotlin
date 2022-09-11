package com.example.simplespringbootkotlin.serialization

import kotlinx.serialization.protobuf.ProtoBuf
import org.springframework.core.codec.DecodingException
import org.springframework.core.io.buffer.DataBuffer
import java.io.BufferedOutputStream
import java.io.OutputStream

/*
 * These can also be accessed through `kotlin.reflect.jvm.internal.impl.protobuf`, not sure
 * if it's ideal or not.
 */

fun ByteArray.writeDelimitedTo(outputStream: OutputStream) {
    val serializedSize = this.size
    val bufferSize = computePreferredBufferSize(computeUInt32SizeNoTag(serializedSize) + serializedSize)
    // not 100% sure it's needed, but at least can enforce the desired buffer-size
    val stream = outputStream.buffered(
        maxOf(
            bufferSize,
            MAX_VARINT_SIZE * 2
        )
    ) // simulate  CodedOutputStream.newInstance(output, bufferSize)
    stream.writeUInt32NoTag(serializedSize) // write sizeTag
    writeTo(stream)
    stream.flush()
}

fun ByteArray.writeTo(outputStream: OutputStream) {
    val bufferSize = computePreferredBufferSize(this.size)
    val stream = outputStream.buffered(
        maxOf(
            bufferSize,
            MAX_VARINT_SIZE * 2
        )
    ) // simulate  CodedOutputStream.newInstance(output, bufferSize)
    stream.write(this) // write actual message
    stream.flush()
}

private const val DEFAULT_BUFFER_SIZE = 4096
private const val MAX_VARINT_SIZE = 10

/** Returns the buffer size to efficiently write dataLength bytes to this CodedOutputStream. */
private fun computePreferredBufferSize(dataLength: Int): Int =
    if (dataLength > DEFAULT_BUFFER_SIZE) DEFAULT_BUFFER_SIZE else dataLength

/** Compute the number of bytes that would be needed to encode an uint32 field. */
private fun computeUInt32SizeNoTag(value: Int): Int = when {
    value and (0.inv() shl 7) == 0 -> 1
    value and (0.inv() shl 14) == 0 -> 2
    value and (0.inv() shl 21) == 0 -> 3
    value and (0.inv() shl 28) == 0 -> 4
    else -> 5
}

fun BufferedOutputStream.writeUInt32NoTag(size: Int) {
    var value = size
    try {
        while (true) {
            if ((value and 0x7F.inv() == 0)) {
                write(value)
                return
            } else {
                write((value and 0x7F) or 0x80)
                value ushr 7
                value = size ushr 7
            }
        }
    } catch (outOfBounds: IndexOutOfBoundsException) {
        throw IllegalArgumentException(outOfBounds)
    }
}

// this should be refactored and placed inside KotlinSerializationProtobufDecoder
private class ProtobufDataBufferDecoder(
    private val protobuf: ProtoBuf,
    private val messageSize: Int = DEFAULT_MESSAGE_MAX_SIZE
) {
    private var offset: Int = 0
    private var messageBytesToRead: Int = 0


    fun <T> decodeDelimitedMessages(): List<T> {
        TODO()
    }

    // similar impl to readMessageSize from ProtobufDecoder.java
    private fun readMessageSize(input: DataBuffer): Boolean {
        if (offset == 0) {
            if (input.readableByteCount() == 0) return false
            val messageSize = input.read().toInt() // should be the message's size
            if (messageSize and 0x80 == 0) { // check if it's positive? Why tho? in case the first byte was not the message size?
                messageBytesToRead = messageSize
                return true
            }
            messageBytesToRead = messageSize and 0x7f // does this drops the msb?
            offset = 7
        }
        if (offset < 32) {
            while (offset < 32) {
                if (input.readableByteCount() == 0) return false
                val messageSize = input.read().toInt()
                // shift the groups of 7 bits  because varints store numbers with the least significant group first
                messageBytesToRead =
                    (messageBytesToRead or messageSize and 0x7f) shl offset // and concatenate them together
                if (messageSize and 0x80 == 0) {
                    offset = 0
                    return true
                }
                offset += 7
            }
        }
        // Keep reading up to 64 bits
        //Note: even though we read more bytes why we do not store their message size?
        while (offset < 64) {
            if (input.readableByteCount() == 0) return false
            val messageSize = input.read().toInt()
            if (messageSize and 0x80 == 0) {
                offset = 0
                return true
            }
            offset += 7
        }
        offset = 0 // is this needed?
        throw DecodingException("Cannot parse message size: malformed varint")
    }

    companion object {
        private const val DEFAULT_MESSAGE_MAX_SIZE = 256 * 1024
    }
}
