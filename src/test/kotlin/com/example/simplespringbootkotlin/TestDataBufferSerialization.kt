@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin

import com.example.simplespringbootkotlin.serialization.decodeFromByteArray
import com.example.simplespringbootkotlin.serialization.decodeFromByteArrayToMono
import com.example.simplespringbootkotlin.serialization.encodeToByteArray
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.util.ConcurrentReferenceHashMap
import java.lang.reflect.Type

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TestDataBufferSerialization {
    private val serializersCache = ConcurrentReferenceHashMap<Type, KSerializer<*>>()

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
        deserializedResult shouldBe input
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
        deserializedResult shouldBe input
    }

    @Test
    fun `should be able to serialize with open-poly with cache`(): Unit = runBlocking {
        val proto = protoBufFormat

        val type: ResolvableType = ResolvableType.forType(Payload::class.java)
        val factory: DataBufferFactory = DefaultDataBufferFactory()
        val input = payload("Hello dataBuffers", "no error")

        val stringSerializer = getSerializer(proto, type.type)

        val buffer = proto.encodeToByteArray(stringSerializer, input, factory)
        val deserializedResult = proto.decodeFromByteArray(stringSerializer, buffer)

        println("Result: $deserializedResult")
        println("Type: $type")
        deserializedResult shouldBe input
    }

    private fun getSerializer(protobuf: ProtoBuf, type: Type): KSerializer<Any> =
        serializersCache.getOrPut(type) {
            protobuf.serializersModule.serializer(type)
        }.cast()

    @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
    private inline fun KSerializer<*>.cast(): KSerializer<Any> {
        return this as KSerializer<Any>
    }


    @Test
    fun `should be able to de-serialize with open-poly to mono`(): Unit = runBlocking {
        val proto = protoBufFormat

        val type: ResolvableType = ResolvableType.forType(Payload::class.java)
        val factory: DataBufferFactory = DefaultDataBufferFactory()
        val input = payload("Hello dataBuffers", "no error")

        val stringSerializer = proto.serializersModule.serializer(type.type)
        val buffer = proto.encodeToByteArray(stringSerializer, input, factory)
        val inputStream = flowOf(buffer).asPublisher(this.coroutineContext)

        val deserializedResult = proto.decodeFromByteArrayToMono(stringSerializer, inputStream)

        val result = deserializedResult.awaitSingle()
        println("Result: $result")
        result shouldBe input
    }
}



