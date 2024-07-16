package com.ribbontek.streamer.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

fun String.toUUID(): UUID = UUID.fromString(this)

fun <T : Any> T.logger(): Logger = LoggerFactory.getLogger(this::class.java)
