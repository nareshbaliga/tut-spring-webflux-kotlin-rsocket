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
import org.springframework.web.reactive.function.client.awaitBody
import reactor.core.publisher.Flux
import reactor.netty.http.client.HttpClient
import java.util.*

@RestController
@RequestMapping("/api/v1/messages")
class MessageResource(val messageService: MessageService) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val webClient =
        WebClient.builder().clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create(
/*
                    ConnectionProvider.builder("myConnectionPool")
                        .maxConnections(500)
                        .pendingAcquireMaxCount(-1)
                        .pendingAcquireTimeout(Duration.ofMillis(180_000))
                        .build()
*/
                ).compress(true).secure()
            )
        )
            .baseUrl("httpbin.org").build()

    @GetMapping
    suspend fun latest(
        @RequestParam(value = "lastMessageId", defaultValue = "") lastMessageId: String
    ): ResponseEntity<List<MessageVM>> {
        logger.debug("Executing latest()")
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
        @RequestParam(value = "delay", defaultValue = "1000") delayMs: Long,
        @RequestParam(value = "block", defaultValue = "false") block: Boolean
    ): ResponseEntity<List<MessageVM>> {
        logger.debug("Executing all() with delay $delayMs")
        if (delayMs >= 1000L) {
            val response = webClient.get().uri("/delay/" + (delayMs / 1000)).retrieve().awaitBody<String>()
            logger.debug(response)
        }
        if (block) {
            val rand = UUID.randomUUID()
            logger.debug("$rand")
        }
        return with(ResponseEntity.ok()) {
            body(
                messageService.all().onEach { messageVM ->
                    logger.debug("Found message $messageVM")
                }
            )
        }
    }

    @GetMapping("/all-flux")
    fun allFlux(
        @RequestParam(value = "delay", defaultValue = "1000") delayMs: Long,
        @RequestParam(value = "block", defaultValue = "false") block: Boolean
    ): Flux<MessageVM> {

        return if (delayMs >= 1000L) {
            webClient.get().uri("/delay/" + (delayMs / 1000)).retrieve().bodyToMono(String::class.java)
                .flatMapMany {
                    logger.debug(it)
                    if (block) {
                        val rand = UUID.randomUUID()
                        logger.debug("$rand")
                    }
                    messageService.allFlux().doOnEach { messageVM ->
                        logger.debug("Found message ${messageVM.get()}")
                    }
                }
        } else {
            messageService.allFlux().doOnEach {
                logger.debug("Found message ${it.get()}")
            }
        }
    }

    @PostMapping
    suspend fun post(@RequestBody message: MessageVM) {
        messageService.post(message)
    }
}
