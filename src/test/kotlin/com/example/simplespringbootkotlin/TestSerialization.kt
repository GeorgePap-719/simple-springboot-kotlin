@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.rsocket.context.LocalRSocketServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveAndAwaitOrNull

@SpringBootTest
class TestSerialization(
    @Autowired
    private val rsocketBuilder: RSocketRequester.Builder,
    @LocalRSocketServerPort
    private val serverPort: String
) {

    @Test
    fun `test proto serialization`(): Unit = runBlocking {
        val payload = payload(TestObject1("hi"), "some error")
        println(protoBufFormat.encodeToByteArray(payload))
    }

    @Test
    fun `test proto serialization with T`(): Unit = runBlocking {
        val payload = payloadT(TestObject1("hi"), "some error T")
        println(protobufFormatWithT.encodeToByteArray(payload))
    }


    @Test
    fun `test serialization in rsocket api`(): Unit = runBlocking {
        val payload = payload(TestObject1("hi"), "some error")

        val tcpRequester = rsocketBuilder
            .tcp("localhost", serverPort.toInt())

        val rsocketResponse = tcpRequester
            .route("put.2")
            .data(protoBufFormat.encodeToByteArray(IncomingMessage("Hi from a message")))
            .retrieveAndAwaitOrNull<ByteArray>()

        rsocketResponse?.let {
            println(protoBufFormat.decodeFromByteArray<IncomingMessage>(it))
        }

    }


    @Test
    fun `test serialization in rsocket api with custom codec support`(): Unit = runBlocking {
        val payload = payload(TestObject1("hi"), "some error")

        val tcpRequester = rsocketBuilder
            .tcp("localhost", serverPort.toInt())

        val rsocketResponse = tcpRequester
            .route("put.3")
            .data(
                payload(IncomingMessage("Hi with payload"), "not error")
            )
            .retrieveAndAwaitOrNull<Payload>()

        rsocketResponse?.let {
            println(it)
        }

    }

}

