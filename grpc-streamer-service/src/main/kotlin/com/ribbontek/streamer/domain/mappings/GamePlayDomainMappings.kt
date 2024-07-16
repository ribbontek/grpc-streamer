package com.ribbontek.streamer.domain.mappings

import com.ribbontek.streamer.domain.DirectionEnum
import com.ribbontek.streamer.domain.PlayerActionEnum
import com.ribbontek.streamer.domain.PlayerCache
import com.ribbontek.streamer.domain.PlayerStateEnum
import com.ribbontek.streamer.domain.PlayerStateEnum.ALIVE
import com.ribbontek.streamer.domain.PlayerStateEnum.DEAD
import com.ribbontek.streamer.domain.PlayerStateEnum.NONE
import com.ribbontek.streamer.domain.PositionCache
import com.ribbontek.stubs.streamer.player.PlayerAction
import com.ribbontek.stubs.streamer.player.PlayerAction.MOVE
import com.ribbontek.stubs.streamer.player.PlayerAction.STRIKE
import com.ribbontek.stubs.streamer.player.PlayerDirection
import com.ribbontek.stubs.streamer.player.PlayerDirection.EAST
import com.ribbontek.stubs.streamer.player.PlayerDirection.NORTH
import com.ribbontek.stubs.streamer.player.PlayerDirection.SOUTH
import com.ribbontek.stubs.streamer.player.PlayerDirection.UNRECOGNIZED
import com.ribbontek.stubs.streamer.player.PlayerDirection.WEST
import com.ribbontek.stubs.streamer.player.PlayerRequest
import com.ribbontek.stubs.streamer.player.PlayerResponse
import com.ribbontek.stubs.streamer.player.PlayerState
import com.ribbontek.stubs.streamer.player.Position
import com.ribbontek.stubs.streamer.player.playerResponse
import com.ribbontek.stubs.streamer.player.position

fun PlayerRequest.toPlayerCache(): PlayerCache {
    return PlayerCache(
        playerId = playerId,
        playerName = playerName,
        position = PositionCache(0, 0, DirectionEnum.NONE),
        action = PlayerActionEnum.NONE,
        state = NONE
    )
}

fun PlayerDirection.toDirectionEnum(): DirectionEnum {
    return when (this) {
        NORTH -> DirectionEnum.NORTH
        EAST -> DirectionEnum.EAST
        SOUTH -> DirectionEnum.SOUTH
        WEST -> DirectionEnum.WEST
        UNRECOGNIZED -> DirectionEnum.NONE
    }
}

fun PlayerAction.toPlayerActionEnum(): PlayerActionEnum {
    return when (this) {
        MOVE -> PlayerActionEnum.MOVE
        STRIKE -> PlayerActionEnum.STRIKE
        PlayerAction.UNRECOGNIZED -> PlayerActionEnum.NONE
    }
}

fun PositionCache.toPosition(): Position {
    val cache = this
    return position {
        this.xPos = cache.x
        this.yPos = cache.y
    }
}

fun PlayerStateEnum.toPlayerState(): PlayerState {
    return when (this) {
        ALIVE -> PlayerState.ALIVE
        DEAD -> PlayerState.DEAD
        NONE -> PlayerState.DEAD
    }
}

fun PlayerState.toPlayerState(): PlayerStateEnum {
    return when (this) {
        PlayerState.ALIVE -> ALIVE
        PlayerState.DEAD -> DEAD
        PlayerState.UNRECOGNIZED -> NONE
    }
}

fun PlayerCache.toPlayer(): PlayerResponse {
    val cache = this
    return playerResponse {
        this.playerId = cache.playerId
        this.playerName = cache.playerName
        this.position = cache.position.toPosition()
        this.state = cache.state.toPlayerState()
    }
}
