@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin.serialization

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
import org.reactivestreams.Publisher
import org.springframework.core.ResolvableType
import org.springframework.core.codec.Decoder
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.http.MediaType
import org.springframework.http.codec.HttpMessageEncoder
import org.springframework.http.codec.protobuf.ProtobufCodecSupport
import org.springframework.util.ConcurrentReferenceHashMap
import org.springframework.util.MimeType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
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
        return protobufSerializer.encodeToDataBufferDelimited(kSerializer, value, bufferFactory)
    }

    override fun encodeValue(
        value: Any,
        bufferFactory: DataBufferFactory,
        valueType: ResolvableType,
        mimeType: MimeType?,
        hints: MutableMap<String, Any>?
    ): DataBuffer {
        val kSerializer = getSerializer(protobufSerializer, valueType.type)
        return protobufSerializer.encodeToDataBuffer(kSerializer, value, bufferFactory)
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
        val decoder = ProtobufDataBufferDecoder(protobufSerializer, maxMessageSize)
        val kSerializer: KSerializer<Any> = getSerializer(protobufSerializer, elementType.type)
        val listSerializer = ListSerializer(kSerializer)
        return inputStream
            .asFlow()
            .transform<DataBuffer, Any> {
                emit(decoder.decodeDelimitedMessages(listSerializer, it))
            }.asFlux()
    }

    override fun decodeToMono(
        inputStream: Publisher<DataBuffer>,
        elementType: ResolvableType,
        mimeType: MimeType?,
        hints: MutableMap<String, Any>?
    ): Mono<Any> {
        val kSerializer = getSerializer(protobufSerializer, elementType.type)
        return protobufSerializer.decodeFromDataBufferToMono(kSerializer, inputStream)
    }

    override fun decode(
        buffer: DataBuffer,
        targetType: ResolvableType,
        mimeType: MimeType?,
        hints: MutableMap<String, Any>?
    ): Any {
        val kSerializer = getSerializer(protobufSerializer, targetType.type)
        return protobufSerializer.decodeFromDataBuffer(kSerializer, buffer)
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
)

private const val DELIMITED_KEY = "delimited" //cannot access 'DELIMITED_KEY', it is package-private

private const val DELIMITED_VALUE = "true" //cannot access 'DELIMITED_VALUE', it is package-private