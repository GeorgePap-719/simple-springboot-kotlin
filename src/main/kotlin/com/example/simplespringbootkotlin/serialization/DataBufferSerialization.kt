@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.protobuf.ProtoBuf
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DataBufferUtils

fun <T> ProtoBuf.encodeToByteArray(
    serializer: SerializationStrategy<T>,
    value: T,
    dataBufferFactory: DataBufferFactory
): DataBuffer {
    val dataBuffer = dataBufferFactory.allocateBuffer()
    try {
        val protoBytes = encodeToByteArray(serializer, value)
        dataBuffer.asOutputStream().write(protoBytes)
        return dataBuffer
    } catch (exception: Exception) {
        DataBufferUtils.release(dataBuffer)
        throw IllegalArgumentException("Unexpected I/O error while writing to data buffer: $exception")
    }
}

fun <T> ProtoBuf.decodeFromByteArray(
    deserializer: DeserializationStrategy<T>,
    dataBuffer: DataBuffer
): T = decodeFromByteArray(deserializer, dataBuffer.asInputStream().readAllBytes())