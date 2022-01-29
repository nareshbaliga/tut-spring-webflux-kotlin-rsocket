package com.example.kotlin.chat.controller

import com.example.kotlin.chat.service.MessageService
import com.example.kotlin.chat.service.MessageVM
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

@RestController
@RequestMapping("/api/v1/messages")
class MessageResource(val messageService: MessageService) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()

    @GetMapping
    fun latest(@RequestParam(value = "lastMessageId", defaultValue = "") lastMessageId: String): ResponseEntity<List<MessageVM>> {
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
    fun all(
        @RequestParam(value = "delay", defaultValue = "1000") delayMs: Long
    ): Iterable<MessageVM> {
        logger.debug("Executing all() with delay $delayMs")

        return messageService.all().onEach {
            if (delayMs >= 1000L) {
                logger.debug("issued httpbin.org/delay")
                val response = restTemplate.exchange(
                    "https://httpbin.org/delay/${delayMs / 1000L}",
                    HttpMethod.GET,
                    null,
                    String::class.java
                )
                logger.debug("response ${response.statusCode}")
            }
            logger.debug("Found Message $it")
        }
    }

    @PostMapping
    fun post(@RequestBody message: MessageVM) {
        messageService.post(message)
    }
}
