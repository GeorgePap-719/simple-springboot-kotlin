@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin

import io.rsocket.metadata.WellKnownMimeType
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.mono
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoBuf
import org.reactivestreams.Publisher
import org.springframework.core.ResolvableType
import org.springframework.core.codec.Decoder
import org.springframework.core.codec.Encoder
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.util.ConcurrentReferenceHashMap
import org.springframework.util.MimeType
import org.springframework.util.MimeTypeUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.reflect.Type

// unused for now.
private val serializersCache = ConcurrentReferenceHashMap<Type, KSerializer<*>>()

// unused for now
private fun getSerializer(protobuf: ProtoBuf, type: Type): KSerializer<Any>? =
    serializersCache.getOrPut(type) {
        protobuf.serializersModule.serializerOrNull(type) ?: return null
    }.cast()

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
private inline fun KSerializer<*>.cast(): KSerializer<Any> {
    return this as KSerializer<Any>
}

class CustomProtobufEncoder(private val protobufSerializer: ProtoBuf) : Encoder<Any> {

    override fun canEncode(elementType: ResolvableType, mimeType: MimeType?): Boolean {
        return supportsMimeType(mimeType)
    }

    override fun encode(
        inputStream: Publisher<out Any>,
        bufferFactory: DataBufferFactory,
        elementType: ResolvableType,
        mimeType: MimeType?,
        hints: MutableMap<String, Any>?
    ): Flux<DataBuffer> {
        return inputStream
            .asFlow()
            .map { encodeDelimitedMessage(it, bufferFactory, elementType) }
            .asFlux()
    }

    override fun encodeValue(
        value: Any,
        bufferFactory: DataBufferFactory,
        valueType: ResolvableType,
        mimeType: MimeType?,
        hints: MutableMap<String, Any>?
    ): DataBuffer {
        val kSerializer = serializer(valueType.type)
        val byteArray = protobufSerializer.encodeToByteArray(kSerializer, value)
        println(byteArray)
        return bufferFactory.wrap(byteArray)
    }

    //https://developers.google.com/protocol-buffers/docs/techniques?hl=en#streaming
    //TODO: support delimited and not delimited values.
    private fun encodeDelimitedMessage(
        value: Any,
        bufferFactory: DataBufferFactory,
        valueType: ResolvableType
    ): DataBuffer {
        val kSerializer = serializer(valueType.type)
        val encodedByteArray = protobufSerializer.encodeToByteArray(kSerializer, value)

        val buffer = bufferFactory.allocateBuffer()
        val serializedSize = encodedByteArray.size
        val outputStream = buffer.asOutputStream()
        outputStream.write(serializedSize)
        outputStream.write(encodedByteArray)
        outputStream.flush()
        return buffer
    }


    override fun getEncodableMimeTypes(): MutableList<MimeType> {
        return mimeTypes.toMutableList()
    }
}


class CustomProtobufDecoder(private val protobufSerializer: ProtoBuf) : Decoder<Any> {
    // default max size for aggregating messages.
    private val maxMessageSize = 256 * 1024

    override fun canDecode(elementType: ResolvableType, mimeType: MimeType?): Boolean {
        return supportsMimeType(mimeType)
    }

    override fun decode(
        inputStream: Publisher<DataBuffer>,
        elementType: ResolvableType,
        mimeType: MimeType?,
        hints: MutableMap<String, Any>?
    ): Flux<Any> {
        return inputStream
            .asFlow()
            .map { decodeDelimitedMessage(inputStream, elementType) }
            .asFlux()
    }

    private suspend fun decodeDelimitedMessage(
        inputStream: Publisher<DataBuffer>,
        valueType: ResolvableType
    ): Any {
        val kSerializer = serializer(valueType.type)
        val dataBuffer = inputStream.awaitSingle()
        // read first byte for getting the message size.
//        getMessageSizeOrNull(dataBuffer) ?: TODO()
        // use the serializer for the rest bytes.
        val byte = dataBuffer.read()
        val byteArray = ByteArray(1)
        byteArray[0] = byte
        val bytesSizeToRead = protobufSerializer.decodeFromByteArray<Int>(byteArray)
        val bytesToWrite = ByteArray(bytesSizeToRead)
        dataBuffer.read(byteArray, 0, bytesSizeToRead)
        return protobufSerializer.decodeFromByteArray(kSerializer, bytesToWrite)
    }

    // TODO: see https://developers.google.com/protocol-buffers/docs/encoding
    private fun getMessageSizeOrNull(dataBuffer: DataBuffer): Int? {
//        val byte = dataBuffer.read()
//        val byteArray = ByteArray(1)
//        byteArray[0] = byte
//        val bytesSizeToRead = protobufSerializer.decodeFromByteArray<Int>(byteArray)
//        val bytesToWrite = ByteArray(bytesSizeToRead)
//        dataBuffer.read(byteArray, 0, bytesSizeToRead)
//        return protobufSerializer.decodeFromByteArray(bytesToWrite)
        return null
    }

    override fun decodeToMono(
        inputStream: Publisher<DataBuffer>,
        elementType: ResolvableType,
        mimeType: MimeType?,
        hints: MutableMap<String, Any>?
    ): Mono<Any> {
        return mono {
            val kSerializer = serializer(elementType.type)
            val dataBuffer = inputStream.awaitSingle()
            protobufSerializer.decodeFromByteArray(kSerializer, dataBuffer.asInputStream().readBytes())
        }
    }

    override fun decode(
        buffer: DataBuffer,
        targetType: ResolvableType,
        mimeType: MimeType?,
        hints: MutableMap<String, Any>?
    ): Any {
        val kSerializer = serializer(targetType.type)
        return protobufSerializer.decodeFromByteArray(kSerializer, buffer.asInputStream().readBytes())
    }

    override fun getDecodableMimeTypes(): MutableList<MimeType> {
        return mimeTypes.toMutableList()
    }
}

private val mimeTypes = listOf(
    MimeType("application", "x-protobuf"),
    MimeType("application", "octet-stream"),
    MimeType("application", "vnd.google.protobuf"),
    MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.string)
)

private fun supportsMimeType(mimeType: MimeType?): Boolean {
    return mimeType?.isPresentIn(mimeTypes) ?: false
}


