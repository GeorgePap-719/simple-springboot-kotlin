package com.example.simplespringbootkotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SimpleSpringbootKotlinApplication

fun main(args: Array<String>) {
    runApplication<SimpleSpringbootKotlinApplication>(*args)
}
