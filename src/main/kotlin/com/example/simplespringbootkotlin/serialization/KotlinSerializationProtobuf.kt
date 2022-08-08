@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin.serialization

import io.rsocket.metadata.WellKnownMimeType
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.asFlux
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
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
private fun getSerializer(protobuf: ProtoBuf, type: Type): KSerializer<Any> =
    serializersCache.getOrPut(type) {
        protobuf.serializersModule.serializer(type)
    }.cast()

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
private inline fun KSerializer<*>.cast(): KSerializer<Any> {
    return this as KSerializer<Any>
}

class KotlinSerializationProtobufEncoder(private val protobufSerializer: ProtoBuf = ProtoBuf) : Encoder<Any> {

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
        val kSerializer = getSerializer(protobufSerializer, valueType.type)
        return protobufSerializer.encodeToByteArray(kSerializer, value, bufferFactory)
    }

    //https://developers.google.com/protocol-buffers/docs/techniques?hl=en#streaming
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


class KotlinSerializationProtobufDecoder(private val protobufSerializer: ProtoBuf = ProtoBuf) : Decoder<Any> {
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
        if (inputStream is Mono) {
            return Flux.from(decodeToMono(inputStream, elementType, mimeType, hints))
        }

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
        val kSerializer = getSerializer(protobufSerializer, elementType.type)
        return protobufSerializer.decodeFromByteArrayToMono(kSerializer, inputStream)
    }


    override fun decode(
        buffer: DataBuffer,
        targetType: ResolvableType,
        mimeType: MimeType?,
        hints: MutableMap<String, Any>?
    ): Any {
        val kSerializer = getSerializer(protobufSerializer, targetType.type)
        return protobufSerializer.decodeFromByteArray(kSerializer, buffer)
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


