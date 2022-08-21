@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin.serialization

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.asFlux
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoBuf
import org.reactivestreams.Publisher
import org.springframework.core.ResolvableType
import org.springframework.core.codec.Decoder
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.http.codec.HttpMessageEncoder
import org.springframework.http.codec.protobuf.ProtobufCodecSupport
import org.springframework.util.ConcurrentReferenceHashMap
import org.springframework.util.MimeType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.IOException
import java.lang.reflect.Type

class KotlinSerializationProtobufEncoder(
    private val protobufSerializer: ProtoBuf = ProtoBuf
) : ProtobufCodecSupport(), HttpMessageEncoder<Any> {
    override fun canEncode(elementType: ResolvableType, mimeType: MimeType?): Boolean =
        serializerOrNull(elementType.type) != null && supportsMimeType(mimeType)

    override fun encode(
        inputStream: Publisher<out Any>,
        bufferFactory: DataBufferFactory,
        elementType: ResolvableType,
        mimeType: MimeType?,
        hints: MutableMap<String, Any>?
    ): Flux<DataBuffer> = inputStream
        .asFlow()
        .map { encodeDelimitedMessage(it, bufferFactory, elementType) }
        .asFlux()

    // see: https://developers.google.com/protocol-buffers/docs/techniques?hl=en#streaming
    private fun encodeDelimitedMessage(
        value: Any,
        bufferFactory: DataBufferFactory,
        valueType: ResolvableType
    ): DataBuffer {
        val kSerializer = serializer(valueType.type)
        val encodedMessage = protobufSerializer.encodeToByteArray(kSerializer, value)
        bufferFactory.allocateBuffer().use { buffer: DataBuffer ->
            val serializedSize = encodedMessage.size
            val outputStream = buffer.asOutputStream()
            outputStream.write(serializedSize) // write first size of message in stream then write the actual message
            outputStream.write(encodedMessage)
            outputStream.flush()
            return buffer
        }
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

    override fun getEncodableMimeTypes(): MutableList<MimeType> = mimeTypes

    override fun getStreamingMediaTypes(): MutableList<MediaType> = _mimeTypes.map {
        MediaType(it.type, it.subtype, mapOf(DELIMITED_KEY to DELIMITED_VALUE))
    }.toMutableList()
}


class KotlinSerializationProtobufDecoder(
    private val protobufSerializer: ProtoBuf = ProtoBuf
) : ProtobufCodecSupport(), Decoder<Any> {
    // default max size for aggregating messages.
    private val maxMessageSize = 256 * 1024

    override fun canDecode(elementType: ResolvableType, mimeType: MimeType?): Boolean =
        serializerOrNull(elementType.type) != null && supportsMimeType(mimeType)

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
        val kSerializer = getSerializer(protobufSerializer, valueType.type)
        val dataBuffer = inputStream.awaitSingle()
        // read first byte for getting the message size.
        val byte = dataBuffer.read()
        val byteArray = ByteArray(1)
        byteArray[0] = byte
        val bytesSizeToRead = protobufSerializer.decodeFromByteArray<Int>(byteArray)
        val bytesToWrite = ByteArray(bytesSizeToRead)
        dataBuffer.read(byteArray, 0, bytesSizeToRead)
        // use the serializer for the rest bytes.
        return protobufSerializer.decodeFromByteArray(kSerializer, bytesToWrite)
    }

    override fun decodeToMono(
        inputStream: Publisher<DataBuffer>,
        elementType: ResolvableType,
        mimeType: MimeType?,
        hints: MutableMap<String, Any>?
    ): Mono<Any> {
        println("Inside decodeTo mono for type: ${elementType.type}")
        val kSerializer = getSerializer(protobufSerializer, elementType.type)
        return protobufSerializer.decodeFromByteArrayToMono(kSerializer, inputStream)
    }

    override fun decode(
        buffer: DataBuffer,
        targetType: ResolvableType,
        mimeType: MimeType?,
        hints: MutableMap<String, Any>?
    ): Any {
//        println("Inside decode for type: ${targetType.type}")
        val kSerializer = getSerializer(protobufSerializer, targetType.type)
        return protobufSerializer.decodeFromByteArray(kSerializer, buffer)
    }

    override fun getDecodableMimeTypes(): MutableList<MimeType> = mimeTypes
}

private val serializersCache = ConcurrentReferenceHashMap<Type, KSerializer<*>>()

private fun getSerializer(protobuf: ProtoBuf, type: Type): KSerializer<Any> =
    serializersCache.getOrPut(type) {
        protobuf.serializersModule.serializer(type)
    }.cast()

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
private inline fun KSerializer<*>.cast(): KSerializer<Any> {
    return this as KSerializer<Any>
}

@Suppress("ObjectPropertyName") // cannot access 'MIME_TYPES', it is package-private
private val _mimeTypes = listOf(
    MimeType("application", "x-protobuf"),
    MimeType("application", "octet-stream"),
    MimeType("application", "vnd.google.protobuf")
//    MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.string) TODO: is this needed?
)

private const val DELIMITED_KEY = "delimited" //cannot access 'DELIMITED_KEY', it is package-private

private const val DELIMITED_VALUE = "true" //cannot access 'DELIMITED_VALUE', it is package-private

private inline fun <D : DataBuffer, R> D.use(block: (D) -> R): R {
    return try {
        block(this)
    } catch (ioException: IOException) {
        DataBufferUtils.release(this)
        throw IllegalArgumentException("Unexpected I/O error while writing to data buffer", ioException)
    }
}

