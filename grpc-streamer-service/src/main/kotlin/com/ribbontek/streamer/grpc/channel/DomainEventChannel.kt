package com.ribbontek.streamer.grpc.channel

import com.ribbontek.streamer.service.DomainEvent
import kotlinx.coroutines.channels.Channel

object DomainEventChannel {
    val channel = Channel<DomainEvent>(Channel.BUFFERED)
}
