package com.example.kotlin.chat

import io.r2dbc.spi.ConnectionFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import org.springframework.stereotype.Component
import reactor.blockhound.BlockHound
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.logging.AccessLog

@SpringBootApplication
class ChatKotlinApplication

fun main(args: Array<String>) {
    System.setProperty("reactor.netty.ioWorkerCount", "1")
    BlockHound.install()
    runApplication<ChatKotlinApplication>(*args)
}

@Configuration
class Config {
    @Bean
    fun initializer(connectionFactory: ConnectionFactory): ConnectionFactoryInitializer {
        val initializer = ConnectionFactoryInitializer()
        initializer.setConnectionFactory(connectionFactory)
        val populator = CompositeDatabasePopulator()
        populator.addPopulators(ResourceDatabasePopulator(ClassPathResource("./sql/schema.sql")))
        initializer.setDatabasePopulator(populator)
        return initializer
    }
}

@Component
class NettyCustomizer(val logCustomizer: LogCustomizer) : WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {
    override fun customize(factory: NettyReactiveWebServerFactory?) {
        factory?.addServerCustomizers(logCustomizer)
    }
}

@Component
class LogCustomizer : NettyServerCustomizer {
    override fun apply(httpServer: HttpServer?): HttpServer? {
        return httpServer?.accessLog(true, { x -> AccessLog.create("method={}, uri={}", x.method(), x.uri()) })
    }
}
