@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin

import com.example.simplespringbootkotlin.serialization.decodeFromByteArray
import com.example.simplespringbootkotlin.serialization.encodeToByteArray
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DefaultDataBufferFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TestDataBufferSerialization {

    @Test
    fun `should be able to serialize`(): Unit = runBlocking {
        val proto = ProtoBuf

        val type: ResolvableType = ResolvableType.forType(String::class.java)
        val factory: DataBufferFactory = DefaultDataBufferFactory()
        val input = "Hello dataBuffers"

        val stringSerializer = proto.serializersModule.serializer(type.type)
        val buffer = proto.encodeToByteArray(stringSerializer, input, factory)

        val deserializedResult = proto.decodeFromByteArray(stringSerializer, buffer)

        println("Result: $deserializedResult")
    }

    @Test
    fun `should be able to serialize with open-poly with no generics`(): Unit = runBlocking {
        val proto = protoBufFormat

        val type: ResolvableType = ResolvableType.forType(Payload::class.java)
        val factory: DataBufferFactory = DefaultDataBufferFactory()
        val input = payload("Hello dataBuffers", "no error")

        val stringSerializer = proto.serializersModule.serializer(type.type)
        val buffer = proto.encodeToByteArray(stringSerializer, input, factory)

        val deserializedResult = proto.decodeFromByteArray(stringSerializer, buffer)

        println("Result: $deserializedResult")
    }
}