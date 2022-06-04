@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.rsocket.context.LocalRSocketServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveAndAwaitOrNull
import org.springframework.messaging.rsocket.sendAndAwait

@SpringBootTest
internal class RSocketControllerTest(
    @Autowired
    private val rsocketBuilder: RSocketRequester.Builder,
    @LocalRSocketServerPort
    private val serverPort: String,
) {

    // https://github.com/spring-projects/spring-framework/issues/26829
    @Test
    fun `test 1st case missing response`(): Unit = runBlocking {
        val tcpRequester = rsocketBuilder.tcp("localhost", serverPort.toInt())
        val message = IncomingMessage("Hi from 1st case")

        tcpRequester
            .route("put")
            .data(protoBufFormat.encodeToByteArray(message))
            .send()
            .block()
    }

    // https://github.com/spring-projects/spring-framework/issues/26829
    @Test
    fun `test 2st case missing response`(): Unit = runBlocking {
        val tcpRequester = rsocketBuilder.tcp("localhost", serverPort.toInt())
        val message = IncomingMessage("Hi from 2st case")

        tcpRequester
            .route("put")
            .data(protoBufFormat.encodeToByteArray(message))
            // pretty similar to the 1st case as both subscribe and wait for the next value.
            .sendAndAwait()
    }

    // workaround for https://github.com/spring-projects/spring-framework/issues/26829
    @Test
    fun `test 1st case where it works`(): Unit = runBlocking {
        val tcpRequester = rsocketBuilder.tcp("localhost", serverPort.toInt())
        val message = IncomingMessage("Hi from working case")

        tcpRequester
            .route("put")
            .data(protoBufFormat.encodeToByteArray(message))
            .retrieveAndAwaitOrNull<Unit>()
    }

}