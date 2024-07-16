package com.ribbontek.streamer.service

import com.ribbontek.streamer.grpc.channel.DomainEventChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.random.Random

data class PlayerStatsEvent(
    val playerId: Int,
    val gameCount: Int,
    val stats: Int
) : DomainEvent

interface DomainEvent

@Component
class MyStatsPublisher {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Scheduled(fixedRate = 100)
    fun publish() {
        runBlocking {
            launch {
                val playerId = Random.nextInt(1, 4)
                log.info("Creating new stats event for player $playerId")
                DomainEventChannel.channel.send(
                    PlayerStatsEvent(
                        playerId = playerId,
                        gameCount = Random.nextInt(1, 100),
                        stats = Random.nextInt(1, 100)
                    )
                )
            }
        }
    }
}
