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
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.util.ConcurrentReferenceHashMap
import org.springframework.util.MimeType
import org.springframework.util.MimeTypeUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.reflect.Type

// unused for now.
private val serializersCache = ConcurrentReferenceHashMap<Type, KSerializer<*>>()
// unsed for now
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
            .map { encodeValue(it, bufferFactory, elementType, mimeType, hints) }
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
    private fun encodeValue() {
        val encodeToByteArray = protobufSerializer.encodeToByteArray("")
        encodeToByteArray.size
    }

    override fun getEncodableMimeTypes(): MutableList<MimeType> {
        return mimeTypes.toMutableList()
    }
}


class CustomProtobufDecoder(private val protobufSerializer: ProtoBuf) : Decoder<Any> {
    override fun canDecode(elementType: ResolvableType, mimeType: MimeType?): Boolean {
        return supportsMimeType(mimeType)
    }

    override fun decode(
        inputStream: Publisher<DataBuffer>,
        elementType: ResolvableType,
        mimeType: MimeType?,
        hints: MutableMap<String, Any>?
    ): Flux<Any> {
        val kSerializer = serializer(elementType.type)

        return inputStream
            .asFlow()
            .map { protobufSerializer.decodeFromByteArray(kSerializer, it.asInputStream().readBytes()) }
            .asFlux()
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


