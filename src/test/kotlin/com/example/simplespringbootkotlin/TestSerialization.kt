@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin

import app.cash.turbine.test
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.rsocket.context.LocalRSocketServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveAndAwaitOrNull
import org.springframework.messaging.rsocket.retrieveFlow

@SpringBootTest
// custom codec support
class TestSerialization(
    @Autowired
    private val rsocketBuilder: RSocketRequester.Builder,
    @LocalRSocketServerPort
    private val serverPort: String
) {
    @Nested
    inner class TestSimpleSerializationWithNoOpenPoly {

        @Test
        fun `test serialization in rsocket put-3 api`(): Unit = runBlocking {
            val message = IncomingMessage("Hi with payload")
            val tcpRequester = rsocketBuilder
                .tcp("localhost", serverPort.toInt())

            val rsocketResponse = tcpRequester
                .route("put.3")
                .data(message)
                .retrieveAndAwaitOrNull<IncomingMessage>()

            rsocketResponse.shouldNotBeNull()
            println(rsocketResponse)
            rsocketResponse shouldBe IncomingMessage("Hi back from put.3 controller")

        }

        @Test
        fun `test serialization in rsocket put api with no response`(): Unit = runBlocking {
            val message = IncomingMessage("Hi with payload")
            val tcpRequester = rsocketBuilder
                .tcp("localhost", serverPort.toInt())

            tcpRequester
                .route("put.3")
                .data(message)
                .retrieveAndAwaitOrNull<Unit>()
        }

        @Test
        fun `test stream serialization in rsocket api`(): Unit = runBlocking {
            val tcpRequester = rsocketBuilder
                .tcp("localhost", serverPort.toInt())
            val rsocketResponse = tcpRequester
                .route("put.stream")
                .data(
                    IncomingMessage("Hi with payload")
                ).retrieveFlow<IncomingMessage>()

            rsocketResponse.collect {
                println(it)
                it shouldBe IncomingMessage("Hi back from put.stream controller")
            }
        }

        @Test
        fun `test two-way channel serialization in rsocket api with ping-pong`(): Unit = runBlocking {
            val tcpRequester = rsocketBuilder
                .tcp("localhost", serverPort.toInt())
            var counter = 0
            val messageFlow = flow {
                for (i in 0..10) emit(IncomingMessage("ping counter: ${++counter}"))
            }

            val response = tcpRequester
                .route("ping.pong")
                .data(messageFlow)
                .retrieveFlow<IncomingMessage>()

            response.collect {
                println(it)
            }
        }


        @Test
        fun `test two-way channel serialization in rsocket api with ping-pong and turbine`(): Unit = runBlocking {
            val tcpRequester = rsocketBuilder
                .tcp("localhost", serverPort.toInt())
            var counter = 0
            val messageFlow = flow {
                for (i in 0..10) emit(IncomingMessage("ping counter: ${++counter}"))
            }

            tcpRequester
                .route("ping.pong")
                .data(messageFlow)
                .retrieveFlow<IncomingMessage>()
                .test {
                    for (incomingMessages in 0 until 10) {
                        println("------ received :${awaitItem()} --------")
                    }
                    awaitComplete()
                }
        }

    }

}

