package com.ribbontek.streamer.exception

import com.ribbontek.streamer.util.logger
import io.grpc.Status
import net.devh.boot.grpc.server.advice.GrpcAdvice
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler
import org.slf4j.Logger

@GrpcAdvice
class ExceptionGrpcAdvice {
    private val log: Logger = logger()

    @GrpcExceptionHandler
    fun handleInvalidArgument(ex: IllegalArgumentException): Status {
        log.error("Handling IllegalArgumentException", ex)
        return Status.INVALID_ARGUMENT.withCause(ex)
    }

    @GrpcExceptionHandler
    fun handleApiException(ex: ApiException): Status {
        log.error("Handling ApiException", ex)
        return when (ex) {
            is NotFoundException -> Status.NOT_FOUND.withDescription(ex.message).withCause(ex)
            is BadRequestException -> Status.FAILED_PRECONDITION.withDescription(ex.message).withCause(ex)
            is ConflictException -> Status.ALREADY_EXISTS.withDescription(ex.message).withCause(ex)
            is AuthenticationException -> Status.PERMISSION_DENIED.withDescription(ex.message).withCause(ex)
            is ValidationException -> Status.INVALID_ARGUMENT.withDescription(ex.message).withCause(ex)
            else -> Status.INTERNAL.withCause(ex)
        }
    }

    @GrpcExceptionHandler
    fun handleException(ex: Throwable): Status {
        log.error("Handling Throwable", ex)
        return Status.INVALID_ARGUMENT.withCause(ex)
    }
}
