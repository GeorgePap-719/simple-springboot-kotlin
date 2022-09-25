@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin

import com.example.simplespringbootkotlin.serialization.decodeFromDataBuffer
import com.example.simplespringbootkotlin.serialization.decodeFromDataBufferToMono
import com.example.simplespringbootkotlin.serialization.encodeToDataBuffer
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
        val buffer = proto.encodeToDataBuffer(stringSerializer, input, factory)

        val deserializedResult = proto.decodeFromDataBuffer(stringSerializer, buffer)

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
        val buffer = proto.encodeToDataBuffer(stringSerializer, input, factory)

        val deserializedResult = proto.decodeFromDataBuffer(stringSerializer, buffer)

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
        val buffer = proto.encodeToDataBuffer(stringSerializer, input, factory)
        val deserializedResult = proto.decodeFromDataBuffer(stringSerializer, buffer)

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
        val buffer = proto.encodeToDataBuffer(stringSerializer, input, factory)
        val inputStream = flowOf(buffer).asPublisher(this.coroutineContext)

        val deserializedResult = proto.decodeFromDataBufferToMono(stringSerializer, inputStream)

        val result = deserializedResult.awaitSingle()
        println("Result: $result")
        result shouldBe input
    }

    @Test
    fun `should be able to serialize with open-poly with cache and explicit types`(): Unit = runBlocking {
        val proto = protoBufFormat
        val type: ResolvableType = ResolvableType.forType(Payload::class.java)
        val factory: DataBufferFactory = DefaultDataBufferFactory()
        val input: Payload = payload("Hello dataBuffers", "no error")

        val stringSerializer = getSerializer(proto, type.type)
        val buffer = proto.encodeToDataBuffer(stringSerializer, input, factory)
        val deserializedResult = proto.decodeFromDataBuffer(stringSerializer, buffer)

        println("Result: $deserializedResult")
        println("Type: $type")
        deserializedResult shouldBe input
    }

    // Inside encodeValue for type: class com.example.simplespringbootkotlin.PayloadImpl
    // Inside decode for type: interface com.example.simplespringbootkotlin.Payload
    //@Test TODO: research solution
    fun `test generic reflection`(): Unit = runBlocking {
        val proto = protoBufFormat
        val type: ResolvableType = ResolvableType.forType(Payload::class.java)
        val typeImpl: ResolvableType = ResolvableType.forType(PayloadImpl::class.java)
        val factory: DataBufferFactory = DefaultDataBufferFactory()
        val input: Payload = payload("Hello dataBuffers", "no error")

        val stringImplSerializer = getSerializer(proto, typeImpl.type)
        val buffer = proto.encodeToDataBuffer(stringImplSerializer, input, factory)

        val stringSerializer = getSerializer(proto, type.type)
        val deserializedResult = proto.decodeFromDataBuffer(stringSerializer, buffer)

        println("Result: $deserializedResult")
        // Type: com.example.simplespringbootkotlin.Payload
        println("Type: $type")
        deserializedResult shouldBe input
    }

    @Test
    fun `test generic reflection2`(): Unit = runBlocking {
        val proto = protoBufFormat
        val type: ResolvableType = ResolvableType.forType(Payload::class.java)
        val typeImpl: ResolvableType = ResolvableType.forType(PayloadImpl::class.java)
        val factory: DataBufferFactory = DefaultDataBufferFactory()
        val input: Payload = payload("Hello dataBuffers", "no error")

        println(input::class.javaObjectType.typeName)

//        val stringImplSerializer = getSerializer(proto, typeImpl.type)
        val buffer = proto.encodeToDataBuffer(proto.serializersModule.serializer(), input, factory)

        val stringSerializer = getSerializer(proto, type.type)
        val deserializedResult = proto.decodeFromDataBuffer(stringSerializer, buffer)

        println("Result: $deserializedResult")
        // Type: com.example.simplespringbootkotlin.Payload
        println("Type: $type")
        deserializedResult shouldBe input
    }
}



