@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin.serialization

import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.protobuf.ProtoBuf
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DataBufferUtils
import reactor.core.publisher.Mono

fun <T> ProtoBuf.encodeToByteArray(
    serializer: SerializationStrategy<T>,
    value: T,
    dataBufferFactory: DataBufferFactory
): DataBuffer {
    val protoBytes = encodeToByteArray(serializer, value)
    // we use wrap() here which does now allocate new memory, that's
    // why we do not need DataBufferUtils.release(buffer).
    return dataBufferFactory.wrap(protoBytes)
}

fun <T> ProtoBuf.decodeFromByteArray(
    deserializer: DeserializationStrategy<T>,
    dataBuffer: DataBuffer
): T {
    try {
        return decodeFromByteArray(deserializer, dataBuffer.asInputStream().readAllBytes())
    } finally {
        DataBufferUtils.release(dataBuffer)
    }
}

fun <T> ProtoBuf.decodeFromByteArrayToMono(
    deserializer: DeserializationStrategy<T>,
    inputStream: Publisher<DataBuffer>
): Mono<T> = mono {
    decodeFromByteArray(deserializer, inputStream.awaitSingle())
}