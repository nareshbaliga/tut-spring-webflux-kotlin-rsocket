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
import reactor.core.publisher.Flux
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

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
        @RequestParam(value = "delay", defaultValue = "1000") delayMs: Long
    ): ResponseEntity<List<MessageVM>> {
        logger.debug("Executing all() with delay $delayMs")
        return with(ResponseEntity.ok()) {
            body(
                messageService.all().onEach {
                    // logger.debug("Should cause blocking error ${UUID.randomUUID()}")
                    if (delayMs >= 1000L) {
                        // logger.debug("issued httpbin.org/delay")
                        webClient.get().uri("/delay/" + (delayMs / 1000)).awaitExchange {
                            logger.debug("response ${it.statusCode()}")
                        }
                    }
                    // logger.debug ("Found Message $it")
                }
            )
        }
    }

    @GetMapping("/all-flux")
    fun allFlux(
        @RequestParam(value = "delay", defaultValue = "1000") delayMs: Long
    ): Flux<MessageVM> {

        return if (delayMs >= 1000L) {
            // logger.debug("issued httpbin.org/delay")
            webClient.get().uri("/delay/" + (delayMs / 1000)).retrieve().bodyToMono(String::class.java)
                .flatMapMany {
                    logger.debug(it)
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
