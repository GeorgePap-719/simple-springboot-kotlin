@file:OptIn(ExperimentalSerializationApi::class)

package com.example.simplespringbootkotlin

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller

val proto = ProtoBuf

@Controller
class RSocketController {

    @MessageMapping("put")
    suspend fun receive(@Payload inBoundMessage: ByteArray) {
        println(inBoundMessage.decode().toString())
    }

    @MessageMapping("put.2")
    suspend fun receive2(@Payload inBoundMessage: ByteArray) {
        decodeWithResponse(inBoundMessage)
    }

}

private fun decodeWithResponse(value: ByteArray): IncomingMessage {
    return proto.decodeFromByteArray(value)
}

private fun ByteArray.decode(): IncomingMessage {
    return proto.decodeFromByteArray(this)
}
