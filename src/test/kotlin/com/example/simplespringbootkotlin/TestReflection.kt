package com.example.simplespringbootkotlin

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
import org.junit.jupiter.api.Test
import org.springframework.core.ResolvableType

class TestReflection {


    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `test reflection capabilities`() {
        val incomingMessage = IncomingMessage("Hi")

        val kClass = incomingMessage::class
        val javaIncomingMessage = incomingMessage::class.java
        val resolvableType = ResolvableType.forRawClass(javaIncomingMessage)

        println("From kclass: ${kClass.simpleName}")
        println("From ResolvableType: ${resolvableType.type}")
        println("From ResolvableType: ${resolvableType}")


        println(Json.serializersModule.serializer(resolvableType.type))

    }
}