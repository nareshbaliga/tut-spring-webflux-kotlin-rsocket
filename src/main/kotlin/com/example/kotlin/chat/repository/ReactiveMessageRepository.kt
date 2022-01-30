package com.example.kotlin.chat.repository

import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface ReactiveMessageRepository : ReactiveCrudRepository<Message, String>
