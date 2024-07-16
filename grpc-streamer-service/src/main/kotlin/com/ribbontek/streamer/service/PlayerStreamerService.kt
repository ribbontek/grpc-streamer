package com.ribbontek.streamer.service

import com.ribbontek.streamer.domain.DirectionEnum
import com.ribbontek.streamer.domain.GamePlayStateCache
import com.ribbontek.streamer.domain.GamePlayStatusEnum
import com.ribbontek.streamer.domain.PlayerActionEnum
import com.ribbontek.streamer.domain.PlayerCache
import com.ribbontek.streamer.domain.PlayerStateEnum.ALIVE
import com.ribbontek.streamer.domain.PlayerStateEnum.DEAD
import com.ribbontek.streamer.domain.PositionCache
import com.ribbontek.streamer.domain.mappings.toDirectionEnum
import com.ribbontek.streamer.domain.mappings.toPlayer
import com.ribbontek.streamer.domain.mappings.toPlayerActionEnum
import com.ribbontek.streamer.domain.mappings.toPlayerCache
import com.ribbontek.streamer.grpc.channel.DomainEventChannel
import com.ribbontek.stubs.streamer.player.MyStatsResponse
import com.ribbontek.stubs.streamer.player.PlayerStreamRequest
import com.ribbontek.stubs.streamer.player.PlayerStreamResponse
import com.ribbontek.stubs.streamer.player.myStatsResponse
import com.ribbontek.stubs.streamer.player.playerStreamResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

@Service
class PlayerStreamerService {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val gamingSessions = ConcurrentHashMap<String, GamePlayStateCache>()

    fun processRequests(requests: Flow<PlayerStreamRequest>): Flow<PlayerStreamResponse> {
        return channelFlow {
            launch {
                requests.collect { request ->
                    gamingSessions.computeIfAbsent(request.sessionId) { _ ->
                        // save new session & player
                        GamePlayStateCache(
                            players = mutableListOf(request.player.toPlayerCache()),
                            status = GamePlayStatusEnum.WAITING,
                            gameGrid = MutableList(GRID_SIZE) { MutableList(GRID_SIZE) { 0 } },
                            currentTurnPlayerId = request.player.playerId
                        )
                    }.update(request).also {
                        send(
                            playerStreamResponse {
                                this.sessionId = request.sessionId
                                this.sessionState = it.status.name
                                this.players.addAll(it.players.map { it.toPlayer() })
                            }
                        )
                    }
                }
            }
        }
    }

    fun getStatsStream(): Flow<MyStatsResponse> {
        // assume auth has authenticated user giving us a player id
        val playerId = Random.nextInt(1, 4)
        log.info("Player Id found: $playerId")
        return channelFlow {
            launch {
                DomainEventChannel.channel.consumeAsFlow().collect { event ->
                    log.info("Listening to event: $event")
                    if (event is PlayerStatsEvent && event.playerId == playerId) {
                        send(
                            myStatsResponse {
                                this.playerId = event.playerId
                                this.stats = event.stats
                                this.gameCount = event.gameCount
                            }
                        )
                    }
                }
            }
        }
    }

    private fun GamePlayStateCache.update(request: PlayerStreamRequest): GamePlayStateCache {
        val gamePlayStateCache = this
        return when (gamePlayStateCache.status) {
            GamePlayStatusEnum.WAITING -> this.waitingStatus(request) // update new player state to DB & cache & allow new players to join
            GamePlayStatusEnum.PLAYING -> this.playingStatus(request) // update positions/directions of all players
            GamePlayStatusEnum.FINISHED -> { // end session updates, save results,
                log.info(gamePlayStateCache.finishMessage())
                gamePlayStateCache
            }
        }
    }

    private fun GamePlayStateCache.waitingStatus(request: PlayerStreamRequest): GamePlayStateCache {
        synchronized(this) {
            log.info("GamePlayStateCache status is WAITING")
            players.find { it.playerId == request.player.playerId }?.run {
                log.info("Player already in GamePlayStateCache")
            } ?: run {
                log.info("Adding new player into GamePlayStateCache")
                players.add(request.player.toPlayerCache())
            }
            if (players.size == 4) { // start game
                log.info("Reached 4 players. GamePlayStateCache status migrating to PLAYING")
                status = GamePlayStatusEnum.PLAYING
                val initPositions = listOf(
                    PositionCache(0, 0, DirectionEnum.SOUTH),
                    PositionCache(GRID_SIZE - 1, 0, DirectionEnum.EAST),
                    PositionCache(0, GRID_SIZE - 1, DirectionEnum.WEST),
                    PositionCache(GRID_SIZE - 1, GRID_SIZE - 1, DirectionEnum.NORTH)
                )
                val usedPositions = mutableListOf<PositionCache>()
                players.forEach {
                    val pos = (initPositions - usedPositions.toSet()).random()
                    it.position = pos
                    usedPositions.add(pos)
                    it.state = ALIVE
                    gameGrid[it.position.x][it.position.y] = it.playerId
                }
            }
            log.info("GamePlayStateCache status is now PLAYING.")
        }
        return this
    }

    private fun GamePlayStateCache.playingStatus(request: PlayerStreamRequest): GamePlayStateCache {
        synchronized(this) {
            log.info("GamePlayStateCache status is PLAYING")
            if (request.player.playerId != currentTurnPlayerId) {
                log.info("Current turn player id is $currentTurnPlayerId")
                if (players.first { it.playerId == currentTurnPlayerId }.state == DEAD) {
                    log.info("Current turn player id $currentTurnPlayerId is DEAD. Re-assigning...")
                    assignNextPlayer()
                }
                return this
            }
            players.first { it.playerId == request.player.playerId && it.state == ALIVE }.run {
                action = request.player.action.toPlayerActionEnum()
                position.direction = request.player.direction.toDirectionEnum()
                when (action) {
                    PlayerActionEnum.MOVE -> movePlayer(this)
                    PlayerActionEnum.STRIKE -> strikePlayer(this)
                    PlayerActionEnum.NONE -> Unit // DO NOTHING
                }
            }
            if (players.count { it.state == ALIVE } == 1) {
                // found the winner! Update game play to finish
                status = GamePlayStatusEnum.FINISHED
                log.info(finishMessage())
            } else {
                log.info("GamePlayStateCache status is still PLAYING.")
            }
        }
        return this
    }

    private fun GamePlayStateCache.finishMessage(): String {
        return "\n*****\nGamePlayStateCache status has FINISHED.\nPlayer ${players.first { it.state == ALIVE }.playerId} is the winner!\n*****"
    }

    private fun GamePlayStateCache.assignNextPlayer() {
        if (players.count { it.state == ALIVE } == 1) {
            return
        }
        val currentIndex = players.indexOfFirst { it.playerId == this.currentTurnPlayerId }
        when {
            // assign to next player in list not dead
            currentIndex != -1 && currentIndex + 1 < players.size -> {
                var nextIndex = (currentIndex + 1)
                for (i in 0 until players.size) {
                    if (players[nextIndex].state == ALIVE) {
                        this.currentTurnPlayerId = players[nextIndex].playerId
                        return
                    }
                    if (nextIndex + 1 < players.size) nextIndex += 1 else nextIndex = 0
                }
            }
            // assign back to first player in list
            else -> {
                this.currentTurnPlayerId = players[0].playerId
            }
        }
    }

    private fun GamePlayStateCache.strikePlayer(playerCache: PlayerCache) {
        val targetPos = playerCache.position.toTargetPosition()
        if (targetPos.x in gameGrid.indices &&
            targetPos.y in gameGrid[0].indices &&
            players.any { it.state == ALIVE && it.position.x == targetPos.x && it.position.y == targetPos.y }
        ) {
            val targetPlayer = players.first { it.state == ALIVE && it.position.x == targetPos.x && it.position.y == targetPos.y }
            targetPlayer.state = DEAD
            log.info("Player ${playerCache.playerId} killed player ${targetPlayer.playerId}")
            gameGrid[targetPos.x][targetPos.y] = 0
            assignNextPlayer()
        } else {
            log.warn("Player ${playerCache.playerId} cannot strike")
        }
    }

    private fun GamePlayStateCache.movePlayer(playerCache: PlayerCache) {
        val targetPos = playerCache.position.toTargetPosition()
        if (targetPos.x in this.gameGrid.indices &&
            targetPos.y in this.gameGrid[0].indices &&
            !players.any { it.state == ALIVE && it.position.x == targetPos.x && it.position.y == targetPos.y }
        ) {
            gameGrid[playerCache.position.x][playerCache.position.y] = 0 // reset current position
            playerCache.position = targetPos
            gameGrid[playerCache.position.x][playerCache.position.y] = playerCache.playerId // update position on grid
            assignNextPlayer()
        } else {
            log.warn("Cannot move Player ${playerCache.playerId}")
        }
    }

    private fun PositionCache.toTargetPosition(): PositionCache {
        return when (this.direction) {
            DirectionEnum.NORTH -> PositionCache(x, y - 1, DirectionEnum.NORTH)
            DirectionEnum.EAST -> PositionCache(x + 1, y, DirectionEnum.EAST)
            DirectionEnum.SOUTH -> PositionCache(x, y + 1, DirectionEnum.SOUTH)
            DirectionEnum.WEST -> PositionCache(x - 1, y, DirectionEnum.WEST)
            else -> this
        }
    }

    companion object {
        const val GRID_SIZE = 5
    }
}
