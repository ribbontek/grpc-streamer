package com.ribbontek.streamer.context

import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration
import net.devh.boot.grpc.client.config.GrpcChannelsProperties
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration
import net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties

@GrpcSpringBootTest
@ImportAutoConfiguration(
    GrpcServerAutoConfiguration::class,
    GrpcServerFactoryAutoConfiguration::class,
    GrpcClientAutoConfiguration::class
)
@EnableConfigurationProperties(GrpcChannelsProperties::class)
abstract class AbstractIntegTest
