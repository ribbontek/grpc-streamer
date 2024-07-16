package com.ribbontek.streamer.grpc

import com.google.protobuf.Empty
import com.ribbontek.streamer.context.AbstractIntegTest
import com.ribbontek.streamer.domain.DirectionEnum.EAST
import com.ribbontek.streamer.domain.DirectionEnum.NONE
import com.ribbontek.streamer.domain.DirectionEnum.NORTH
import com.ribbontek.streamer.domain.DirectionEnum.SOUTH
import com.ribbontek.streamer.domain.DirectionEnum.WEST
import com.ribbontek.streamer.domain.GamePlayStateCache
import com.ribbontek.streamer.domain.GamePlayStatusEnum
import com.ribbontek.streamer.domain.PlayerStateEnum.ALIVE
import com.ribbontek.streamer.domain.PositionCache
import com.ribbontek.streamer.domain.mappings.toPlayerCache
import com.ribbontek.streamer.domain.mappings.toPlayerState
import com.ribbontek.streamer.service.PlayerStreamerService.Companion.GRID_SIZE
import com.ribbontek.streamer.util.FakerUtil
import com.ribbontek.streamer.util.PlayerTestUtils.move
import com.ribbontek.streamer.util.PlayerTestUtils.shortestPath
import com.ribbontek.streamer.util.PlayerTestUtils.strike
import com.ribbontek.streamer.util.PlayerTestUtils.toPlayerDirection
import com.ribbontek.stubs.streamer.player.MyStatsResponse
import com.ribbontek.stubs.streamer.player.PlayerDirection.UNRECOGNIZED
import com.ribbontek.stubs.streamer.player.PlayerRequest
import com.ribbontek.stubs.streamer.player.PlayerStreamRequest
import com.ribbontek.stubs.streamer.player.PlayerStreamResponse
import com.ribbontek.stubs.streamer.player.PlayerStreamServiceGrpcKt.PlayerStreamServiceCoroutineStub
import com.ribbontek.stubs.streamer.player.playerRequest
import com.ribbontek.stubs.streamer.player.playerStreamRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import net.devh.boot.grpc.client.inject.GrpcClient
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class PlayerStreamGrpcServiceIntegTest : AbstractIntegTest() {
    @GrpcClient("playerStreamServiceCoroutineStub")
    private lateinit var playerStreamServiceCoroutineStub: PlayerStreamServiceCoroutineStub

    @GrpcClient("playerStreamServiceCoroutineStub")
    private lateinit var playerStreamServiceCoroutineStub1: PlayerStreamServiceCoroutineStub

    @GrpcClient("playerStreamServiceCoroutineStub")
    private lateinit var playerStreamServiceCoroutineStub2: PlayerStreamServiceCoroutineStub

    @GrpcClient("playerStreamServiceCoroutineStub")
    private lateinit var playerStreamServiceCoroutineStub3: PlayerStreamServiceCoroutineStub

    @GrpcClient("playerStreamServiceCoroutineStub")
    private lateinit var playerStreamServiceCoroutineStub4: PlayerStreamServiceCoroutineStub

    private val log = LoggerFactory.getLogger(this::class.java)

    @Test
    fun `getPlayerStream - multiple streams & players`() {
        runBlocking {
            val sessionId = UUID.randomUUID().toString() // new session

            val player1 = PlayerStreamer(createPlayer(1), playerStreamServiceCoroutineStub1)
            val player2 = PlayerStreamer(createPlayer(2), playerStreamServiceCoroutineStub2)
            val player3 = PlayerStreamer(createPlayer(3), playerStreamServiceCoroutineStub3)
            val player4 = PlayerStreamer(createPlayer(4), playerStreamServiceCoroutineStub4)

            player1.send(sessionId)
            player2.send(sessionId)

            val threeStreams = merge(
                player1.createStream(),
                player2.createStream(),
                player3.createStream()
            )

            delay(1000)
            player3.send(sessionId)
            delay(1000)
            player4.send(sessionId)

            val combinedStreams = merge(
                threeStreams,
                player4.createStream()
            )
            val gamePlayStateCache = GamePlayStateCache(
                players = mutableListOf(
                    player1.playerRequest.toPlayerCache(),
                    player2.playerRequest.toPlayerCache(),
                    player3.playerRequest.toPlayerCache(),
                    player4.playerRequest.toPlayerCache()
                ),
                status = GamePlayStatusEnum.PLAYING, // set immediately to playing
                gameGrid = MutableList(GRID_SIZE) { MutableList(GRID_SIZE) { 0 } },
                currentTurnPlayerId = 0 // we don't care about this
            )

            val job = async {
                combinedStreams.collect {
                    gamePlayStateCache.status = GamePlayStatusEnum.valueOf(it.sessionState)
                    gamePlayStateCache.gameGrid = MutableList(GRID_SIZE) { MutableList(GRID_SIZE) { 0 } }
                    it.playersList.forEach { player ->
                        gamePlayStateCache.players.first {
                            it.playerId == player.playerId
                        }.apply {
                            this.position = PositionCache(player.position.xPos, player.position.yPos, NONE)
                            this.state = player.state.toPlayerState()
                            if (state == ALIVE) {
                                gamePlayStateCache.gameGrid[player.position.xPos][player.position.yPos] = player.playerId
                            }
                        }
                    }
                    log.info("GRID UPDATE: \n" + gamePlayStateCache.printGridV2())
                }
            }

            val playerActionsJob = async {
                while (gamePlayStateCache.status == GamePlayStatusEnum.PLAYING) {
                    listOf(player1, player2, player3, player4).forEach { player ->
                        gamePlayStateCache.players.firstOrNull {
                            it.playerId == player.playerRequest.playerId && it.state == ALIVE
                        }?.let { selectedPlayer ->
                            val targetStrikePositions = arrayOf(
                                PositionCache(selectedPlayer.position.x, selectedPlayer.position.y - 1, NORTH),
                                PositionCache(selectedPlayer.position.x + 1, selectedPlayer.position.y, EAST),
                                PositionCache(selectedPlayer.position.x, selectedPlayer.position.y + 1, SOUTH),
                                PositionCache(selectedPlayer.position.x - 1, selectedPlayer.position.y, WEST)
                            )

                            gamePlayStateCache.players.firstOrNull {
                                it.playerId != player.playerRequest.playerId && it.state == ALIVE &&
                                    targetStrikePositions.any { tarPos -> tarPos.x == it.position.x && tarPos.y == it.position.y }
                            }?.let { targetPlayer ->
                                val target = targetStrikePositions.first { tarPos ->
                                    tarPos.x == targetPlayer.position.x && tarPos.y == targetPlayer.position.y
                                }
                                log.info("Player ${selectedPlayer.playerId} Striking ${target.direction.toPlayerDirection()} at target player ${targetPlayer.playerId}")
                                player.send(sessionId, player.playerRequest.strike(target.direction.toPlayerDirection()))
                                delay(100)
                            } ?: run {
                                // otherwise move player to attack random player
                                val target = gamePlayStateCache.players.filter { it.playerId != player.playerRequest.playerId && it.state == ALIVE }.random()
                                shortestPath(MutableList(GRID_SIZE) { MutableList(GRID_SIZE) { 0 } }, selectedPlayer.position, target.position)
                                    ?.takeIf { it.toPlayerDirection() != UNRECOGNIZED }
                                    ?.let {
                                        log.info("Player ${selectedPlayer.playerId} moving ${it.toPlayerDirection()} at target player ${selectedPlayer.playerId}")
                                        player.send(sessionId, player.playerRequest.move(it.toPlayerDirection()))
                                        delay(100)
                                    }
                                    ?: run {
                                        log.info("Unable to find shortest path between ${selectedPlayer.position} & ${target.position}")
                                    }
                            }
                        } ?: run {
                            log.info("Player ${player.playerRequest.playerId} is ${gamePlayStateCache.players.firstOrNull { it.playerId == player.playerRequest.playerId }?.state}")
                        }
                    }
                    delay(100)
                }
            }

            do {
                log.info("POLLING STATUS 10 SEC")
                delay(10000)
            } while (gamePlayStateCache.status != GamePlayStatusEnum.FINISHED)
            // cancel streams collector
            job.cancelAndJoin()
            // cancel player actions job
            playerActionsJob.cancelAndJoin()
        }
    }

    @Test
    fun `getStatsStream - stream random stats over 1 min`() {
        runBlocking {
            val stats = mutableListOf<MyStatsResponse>()
            val job = async {
                playerStreamServiceCoroutineStub.getStatsStream(Empty.getDefaultInstance()).collect {
                    log.info("Player ${it.playerId} with total games: ${it.gameCount} and stats: ${it.stats}")
                    stats.add(it)
                }
            }
            waitForConditionWithTimeout(1.minutes) {
                log.info("Stream player stats size: ${stats.size}")
                stats.size >= 100
            }
            job.cancelAndJoin()
        }
    }

    private suspend fun waitForCondition(condition: () -> Boolean) {
        val mutex = Mutex()
        while (true) {
            mutex.withLock {
                if (condition()) {
                    return
                }
            }
            delay(1000)
        }
    }

    private suspend fun waitForConditionWithTimeout(timeout: Duration, condition: () -> Boolean) {
        withTimeout(timeout) {
            waitForCondition(condition)
        }
    }

    private data class PlayerStreamer(
        val playerRequest: PlayerRequest,
        val playerStreamServiceCoroutineStub: PlayerStreamServiceCoroutineStub
    ) {
        private val log = LoggerFactory.getLogger(this::class.java)
        private val channel: Channel<PlayerStreamRequest> = Channel(Channel.BUFFERED)

        suspend fun send(sessionId: String, newPlayerRequest: PlayerRequest? = null) = coroutineScope {
            launch {
                channel.send(
                    newPlayerRequest?.let {
                        playerStreamRequest {
                            this.sessionId = sessionId
                            this.player = it
                        }
                    } ?: playerStreamRequest {
                        this.sessionId = sessionId
                        this.player = playerRequest
                    }
                )
            }
        }

        suspend fun createStream(): Flow<PlayerStreamResponse> = coroutineScope {
            flow {
                playerStreamServiceCoroutineStub.getPlayerStream(channel.consumeAsFlow()).collect {
                    log.info("Session: ${it.sessionId}, state: ${it.sessionState}, players: ${it.playersCount}")
                    emit(it)
                }
            }
        }
    }

    private fun createPlayer(playerId: Int): PlayerRequest {
        return playerRequest {
            this.playerId = playerId
            this.playerName = FakerUtil.firstName()
        }
    }
}
