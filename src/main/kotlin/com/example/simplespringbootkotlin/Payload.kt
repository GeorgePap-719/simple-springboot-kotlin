package com.example.simplespringbootkotlin

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable


interface Payload {
    val data: Any
    val error: String
}

@Serializable
data class PayloadImpl(@Polymorphic override val data: Any, override val error: String) : Payload

fun payload(data: Any, error: String): Payload {
    return PayloadImpl(data, error)
}

@Serializable
data class TestObject1(val value: String)

interface PayloadWithT<T> {
    val data: T
    val error: String
}

@Serializable
data class PayloadTImpl<T>(override val data: T, override val error: String) : PayloadWithT<T>

fun <T> payloadT(data: T, error: String): PayloadWithT<T> {
    return PayloadTImpl(data, error)
}