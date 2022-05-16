package com.example.simplespringbootkotlin

import kotlinx.serialization.Serializable

@Serializable
data class IncomingMessage(val data: String)
