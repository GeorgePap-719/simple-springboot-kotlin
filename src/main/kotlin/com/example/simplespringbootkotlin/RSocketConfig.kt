@file:OptIn(ExperimentalSerializationApi::class, ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin

import com.example.simplespringbootkotlin.serialization.KotlinSerializationProtobufDecoder
import com.example.simplespringbootkotlin.serialization.KotlinSerializationProtobufEncoder
import kotlinx.serialization.ExperimentalSerializationApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler

@Configuration
class RSocketConfig {

    @Bean
    fun rsocketMessageHandler(strategyForPayload: RSocketStrategies) = RSocketMessageHandler().apply {
        rSocketStrategies = strategyForPayload

    }

    @Bean
    fun strategyForPayload(): RSocketStrategies {
        return RSocketStrategies
            .builder()
            .encoder(KotlinSerializationProtobufEncoder(protoBufFormat))
            .decoder(KotlinSerializationProtobufDecoder(protoBufFormat))
//            .encoder(ProtobufEncoder())
//            .decoder(ProtobufDecoder())
            .build()

    }

}












