package com.ribbontek.streamer.util

import com.ribbontek.streamer.domain.DirectionEnum
import com.ribbontek.streamer.domain.DirectionEnum.EAST
import com.ribbontek.streamer.domain.DirectionEnum.NONE
import com.ribbontek.streamer.domain.DirectionEnum.NORTH
import com.ribbontek.streamer.domain.DirectionEnum.SOUTH
import com.ribbontek.streamer.domain.DirectionEnum.WEST
import com.ribbontek.streamer.domain.PositionCache
import com.ribbontek.stubs.streamer.player.PlayerAction.MOVE
import com.ribbontek.stubs.streamer.player.PlayerAction.STRIKE
import com.ribbontek.stubs.streamer.player.PlayerDirection
import com.ribbontek.stubs.streamer.player.PlayerDirection.UNRECOGNIZED
import com.ribbontek.stubs.streamer.player.PlayerRequest
import com.ribbontek.stubs.streamer.player.copy
import java.util.LinkedList
import java.util.Queue

object PlayerTestUtils {

    fun DirectionEnum.toPlayerDirection(): PlayerDirection {
        return when (this) {
            NORTH -> PlayerDirection.NORTH
            EAST -> PlayerDirection.EAST
            SOUTH -> PlayerDirection.SOUTH
            WEST -> PlayerDirection.WEST
            NONE -> UNRECOGNIZED
        }
    }

    fun PlayerRequest.strike(direction: PlayerDirection): PlayerRequest {
        return this.copy {
            this.direction = direction
            this.action = STRIKE
        }
    }

    fun PlayerRequest.move(direction: PlayerDirection): PlayerRequest {
        return this.copy {
            this.direction = direction
            this.action = MOVE
        }
    }

    fun shortestPath(grid: MutableList<MutableList<Int>>, start: PositionCache, target: PositionCache): DirectionEnum? {
        val directions = arrayOf(
            PositionCache(0, -1, NORTH),
            PositionCache(1, 0, EAST),
            PositionCache(0, 1, SOUTH),
            PositionCache(-1, 0, WEST)
        )

        val queue: Queue<PositionCache> = LinkedList()
        val visited = MutableList(grid.size) { MutableList(grid[0].size) { false } }
        val parent = mutableMapOf<PositionCache, PositionCache>()

        queue.add(start)
        visited[start.x][start.y] = true

        while (queue.isNotEmpty()) {
            val current = queue.poll()

            if (current.x == target.x && current.y == target.y) {
                // Backtrack to find the first move
                var firstMove = current
                while (parent[firstMove] != start) {
                    firstMove = parent[firstMove]!!
                }
                return firstMove.direction
            }

            for (direction in directions) {
                val next = PositionCache(current.x + direction.x, current.y + direction.y, direction.direction)

                if (isValidMove(grid, visited, next.x, next.y)) {
                    queue.add(next)
                    visited[next.x][next.y] = true
                    parent[next] = current
                }
            }
        }
        return null
    }

    private fun isValidMove(
        grid: MutableList<MutableList<Int>>,
        visited: MutableList<MutableList<Boolean>>,
        row: Int,
        col: Int
    ): Boolean {
        return row >= 0 && row < grid.size &&
            col >= 0 && col < grid[0].size &&
            grid[row][col] == 0 && !visited[row][col]
    }
}
