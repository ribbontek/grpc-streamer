package com.ribbontek.streamer.grpc

import com.google.protobuf.Empty
import com.ribbontek.streamer.service.PlayerStreamerService
import com.ribbontek.stubs.streamer.player.MyStatsResponse
import com.ribbontek.stubs.streamer.player.PlayerStreamRequest
import com.ribbontek.stubs.streamer.player.PlayerStreamResponse
import com.ribbontek.stubs.streamer.player.PlayerStreamServiceGrpcKt.PlayerStreamServiceCoroutineImplBase
import kotlinx.coroutines.flow.Flow
import net.devh.boot.grpc.server.service.GrpcService

@GrpcService
class PlayerStreamGrpcService(
    private val playerStreamerService: PlayerStreamerService
) : PlayerStreamServiceCoroutineImplBase() {
    override fun getPlayerStream(requests: Flow<PlayerStreamRequest>): Flow<PlayerStreamResponse> {
        return playerStreamerService.processRequests(requests)
    }

    override fun getStatsStream(request: Empty): Flow<MyStatsResponse> {
        return playerStreamerService.getStatsStream()
    }
}
