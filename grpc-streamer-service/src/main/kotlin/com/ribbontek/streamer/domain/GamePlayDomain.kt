package com.ribbontek.streamer.domain

data class GamePlayStateCache(
    val players: MutableList<PlayerCache>,
    var currentTurnPlayerId: Int,
    var status: GamePlayStatusEnum,
    var gameGrid: MutableList<MutableList<Int>> = mutableListOf(mutableListOf())
) {
    fun printGridV2(): String {
        val transposedGrid = gameGrid[0].indices.map { colIndex ->
            gameGrid.map { row -> row[colIndex] }
        }
        return transposedGrid.joinToString(separator = "\n") { row ->
            row.joinToString(separator = " ") { cell ->
                if (cell == 0) "." else cell.toString()
            }
        }
    }
}

enum class GamePlayStatusEnum {
    WAITING, PLAYING, FINISHED
}

enum class DirectionEnum {
    NORTH, EAST, SOUTH, WEST, NONE
}

enum class PlayerActionEnum {
    MOVE, STRIKE, NONE
}

enum class PlayerStateEnum {
    ALIVE, DEAD, NONE
}

data class PositionCache(
    var x: Int,
    var y: Int,
    var direction: DirectionEnum
)

data class PlayerCache(
    val playerId: Int,
    val playerName: String,
    var position: PositionCache,
    var action: PlayerActionEnum,
    var state: PlayerStateEnum
)
