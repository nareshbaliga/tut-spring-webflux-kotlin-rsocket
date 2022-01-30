package com.example.kotlin.chat.service

import com.example.kotlin.chat.repository.Message
import reactor.core.publisher.Flux

interface MessageService {

    suspend fun latest(): List<MessageVM>

    suspend fun after(messageId: String): List<MessageVM>

    suspend fun post(message: MessageVM)

    suspend fun all(): List<MessageVM>

    fun allFlux(): Flux<MessageVM>
}
