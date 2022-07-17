package com.example.simplespringbootkotlin

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.Connection
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

@RestController
class SimpleRestController {


    @GetMapping(value = ["/api/", "/api"])
    suspend fun checkString(@PathVariable input: String?): String {
        println("received request: $input")

        return "The format is good"
    }
}

@Configuration
class WebClientConfiguration {

    @Bean("WebClient")
    fun webClient(): WebClient =
        WebClient.builder().apply {
            val httpClient = HttpClient.create().apply {
                option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT)
                responseTimeout(Duration.ofMillis(RESPONSE_TIMEOUT))
                doOnConnected { connection: Connection ->
                    connection.addHandlerLast(
                        ReadTimeoutHandler(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                    ).addHandlerLast(WriteTimeoutHandler(WRITE_TIMEOUT, TimeUnit.MILLISECONDS))
                }
            }
            it.clientConnector(ReactorClientHttpConnector(httpClient))
        }.build()

    private companion object {
        const val CONNECTION_TIMEOUT: Int = 5000
        const val RESPONSE_TIMEOUT: Long = 5000
        const val READ_TIMEOUT: Long = 5000
        const val WRITE_TIMEOUT: Long = 5000
    }
}