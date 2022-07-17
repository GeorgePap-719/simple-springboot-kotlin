package com.example.simplespringbootkotlin

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

@SpringBootTest
class TestSimpleController(@Autowired private val webClient: WebClient) {


    @Test
    fun `check path variable decoding`(): Unit = runBlocking {
        val input = "%$@3"

        webClient
            .get()
            .uri("/api/in")
            .awaitExchange {
                println("status code: $it.statusCode()")
                val awaitBody = it.awaitBody(String::class)
                println("response $awaitBody")
            }

//        val response = webClient
//            .get()
//            .uri("/api/in/")
//            .accept(MediaType.APPLICATION_JSON)
//            .retrieve()
//            .awaitBody<String>()

//        println("received response: $response")
    }

}