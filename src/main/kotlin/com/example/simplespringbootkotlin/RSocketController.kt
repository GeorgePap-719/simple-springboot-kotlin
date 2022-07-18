@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.protobuf.ProtoBuf
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import org.springframework.messaging.handler.annotation.Payload as SpringPayload

val protoBufFormat = ProtoBuf {
    serializersModule = SerializersModule {

        polymorphic(Payload::class) {
            subclass(PayloadImpl::class, PayloadImpl.serializer())
            polymorphic(Any::class) {
                subclass(TestObject1::class, TestObject1.serializer())
                subclass(IncomingMessage::class, IncomingMessage.serializer())
            }
        }
    }
}

val protobufFormatWithT = ProtoBuf {
    serializersModule = SerializersModule {
        polymorphic(PayloadWithT::class) {
            subclass(PayloadTImpl.serializer(PolymorphicSerializer(Any::class)))
            polymorphic(Any::class) {
                subclass(TestObject1::class, TestObject1.serializer())
                subclass(IncomingMessage::class, IncomingMessage.serializer())
            }
        }
    }
}

@Controller
// no open-poly serialization
class RSocketController {
    private var counter = 0

    @MessageMapping("put")
    suspend fun receive(@SpringPayload inBoundMessage: IncomingMessage) {
        println("received in put: $inBoundMessage")
    }

    @MessageMapping("put.3")
    suspend fun receive3(@SpringPayload inBoundMessage: IncomingMessage): IncomingMessage {
        println("received in put.3: $inBoundMessage")
        return IncomingMessage("Hi back from put.3 controller")
    }

    @MessageMapping("put.stream")
    fun stream(@SpringPayload inBoundMessage: IncomingMessage): Flow<IncomingMessage> {
        println("received in put.stream: $inBoundMessage")
        return flowOf(IncomingMessage("Hi back from put.stream controller"))
    }

    @MessageMapping("ping.pong")
    fun pingPong(@SpringPayload inBoundMessage: Flow<IncomingMessage>): Flow<IncomingMessage> {
        return flow {
            inBoundMessage.collect {
                if (counter == 10) return@collect
                counter++
                println("received: $it")
                emit(IncomingMessage("Pong-$counter"))
            }
        }
    }
}
