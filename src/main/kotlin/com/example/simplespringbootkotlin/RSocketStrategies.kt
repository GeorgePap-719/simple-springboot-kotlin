@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import org.springframework.http.codec.protobuf.ProtobufCodecSupport
import org.springframework.http.codec.protobuf.ProtobufEncoder
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler

@Configuration
class RSocketStrategies : ProtobufCodecSupport() {

    @Bean
    fun rsocketMessageHandler(strategyForPayload: RSocketStrategies) = RSocketMessageHandler().apply {
        rSocketStrategies = strategyForPayload

    }

    @Bean
    fun strategyForPayload(): RSocketStrategies {
        return RSocketStrategies
            .builder()
            .encoder(CustomProtobufEncoder(ProtoBuf))
            .decoder(CustomProtobufDecoder(ProtoBuf))
//            .encoder(ProtobufEncoder())
//            .encoder(KotlinSerializationJsonEncoder())
//            .encoder(KotlinSerializationJsonEncoder())
//            .decoder(ProtobufDecoder())
//            .decoder(KotlinSerializationJsonDecoder)
            .build()

    }

}












