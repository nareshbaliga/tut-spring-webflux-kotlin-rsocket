package com.example.kotlin.chat.controller

import com.example.kotlin.chat.service.MessageService
import com.example.kotlin.chat.service.MessageVM
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import reactor.netty.http.client.HttpClient

@RestController
@RequestMapping("/api/v1/messages")
class MessageResource(val messageService: MessageService) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val webClient =
        WebClient.builder().clientConnector(ReactorClientHttpConnector(HttpClient.create().compress(true).secure()))
            .baseUrl("httpbin.org").build()

    @GetMapping
    suspend fun latest(
        @RequestParam(value = "lastMessageId", defaultValue = "") lastMessageId: String
    ): ResponseEntity<List<MessageVM>> {
        val messages = if (lastMessageId.isNotEmpty()) {
            messageService.after(lastMessageId)
        } else {
            messageService.latest()
        }

        return if (messages.isEmpty()) {
            with(ResponseEntity.noContent()) {
                header("lastMessageId", lastMessageId)
                build<List<MessageVM>>()
            }
        } else {
            with(ResponseEntity.ok()) {
                header("lastMessageId", messages.last().id)
                body(messages)
            }
        }
    }

    @GetMapping("/all")
    suspend fun all(
        @RequestParam(value = "delay", defaultValue = "1000") delayMs: Long
    ): ResponseEntity<List<MessageVM>> {
        logger.info("Executing all() with delay $delayMs")
        return with(ResponseEntity.ok()) {
            body(
                messageService.all().onEach {
                    logger.info("issued httpbin.org/delay")
                    webClient.get().uri("/delay/" + (delayMs / 1000)).awaitExchange {
                        logger.info("response ${it.statusCode()}")
                    }
                    logger.info("Found Message $it")
                }
            )
        }
    }

    @PostMapping
    suspend fun post(@RequestBody message: MessageVM) {
        messageService.post(message)
    }
}
