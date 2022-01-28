package com.example.kotlin.chat.service

import com.example.kotlin.chat.asDomainObject
import com.example.kotlin.chat.mapToViewModel
import com.example.kotlin.chat.repository.MessageRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class PersistentMessageService(val messageRepository: MessageRepository) : MessageService {

    override suspend fun latest(): List<MessageVM> =
        messageRepository.findLatest()
            .mapToViewModel()

    override suspend fun after(messageId: String): List<MessageVM> =
        messageRepository.findLatest(messageId)
            .mapToViewModel()

    override suspend fun post(message: MessageVM) {
        messageRepository.save(message.asDomainObject())
    }

    override suspend fun all(): List<MessageVM> {
        return messageRepository.findAll().toList().mapToViewModel()
    }
}