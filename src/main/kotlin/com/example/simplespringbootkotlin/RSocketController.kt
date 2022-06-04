@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
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
            }
        }
    }
}

@Controller
class RSocketController {

    @MessageMapping("put")
    suspend fun receive(@SpringPayload inBoundMessage: IncomingMessage) {
        println(inBoundMessage.toString())
    }

    @MessageMapping("put.2")
    suspend fun receive2(@SpringPayload inBoundMessage: ByteArray): ByteArray {
        val response = decodeWithResponse(inBoundMessage)
        println(response)
        return protoBufFormat.encodeToByteArray(IncomingMessage("Hi back with byte array response"))
    }


    @MessageMapping("put.3")
    suspend fun receive3(@SpringPayload inBoundMessage: Payload): Payload {
        println(inBoundMessage.toString())
        return payload(TestObject1("hi back with codec support"), "no error here")
    }

}

private fun decodeWithResponse(value: ByteArray): IncomingMessage {
    return protoBufFormat.decodeFromByteArray(value)
}

private fun ByteArray.decode(): IncomingMessage {
    return protoBufFormat.decodeFromByteArray(this)
}
