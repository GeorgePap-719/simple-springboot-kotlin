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
import org.springframework.util.FastByteArrayOutputStream
import reactor.core.publisher.Mono
import java.io.IOException

fun <T> ProtoBuf.encodeToDataBuffer(
    serializer: SerializationStrategy<T>,
    value: T,
    dataBufferFactory: DataBufferFactory
): DataBuffer = try {
    val outputStream = FastByteArrayOutputStream()
    val protoBytes = encodeToByteArray(serializer, value)
    protoBytes.writeTo(outputStream)
    // use wrap() here which does now allocate new memory, that's
    // why we do not need DataBufferUtils.release(buffer).
    dataBufferFactory.wrap(outputStream.toByteArrayUnsafe())
} catch (ioException: IOException) {
    throw IllegalStateException("Unexpected I/O error while writing to data buffer", ioException)
}

fun <T> ProtoBuf.encodeToDataBufferDelimited(
    serializer: SerializationStrategy<T>,
    value: T,
    dataBufferFactory: DataBufferFactory
): DataBuffer = try {
    val outputStream = FastByteArrayOutputStream()
    val protoBytes = encodeToByteArray(serializer, value)
    protoBytes.writeDelimitedTo(outputStream)
    // use wrap() here which does now allocate new memory, that's
    // why we do not need DataBufferUtils.release(buffer).
    dataBufferFactory.wrap(outputStream.toByteArrayUnsafe())
} catch (ioException: IOException) {
    throw IllegalStateException("Unexpected I/O error while writing to data buffer", ioException)
}

// does not propagate exceptions like ProtobufDecoder.java
// for a more unified message error handling.
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