@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.protobuf.ProtoBuf
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory

fun <T> ProtoBuf.encodeToByteArray(
    serializer: SerializationStrategy<T>,
    value: T,
    dataBufferFactory: DataBufferFactory
): DataBuffer {
    val protoBytes = encodeToByteArray(serializer, value)
    return dataBufferFactory.wrap(protoBytes)
}

fun <T> ProtoBuf.decodeFromByteArray(
    deserializer: DeserializationStrategy<T>,
    dataBuffer: DataBuffer
): T = decodeFromByteArray(deserializer, dataBuffer.asInputStream().readAllBytes())