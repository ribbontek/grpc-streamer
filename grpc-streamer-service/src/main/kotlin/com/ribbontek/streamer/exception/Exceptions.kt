package com.ribbontek.streamer.exception

abstract class ApiException(message: String) : RuntimeException(message)

class NotFoundException(message: String) : ApiException(message)

class AlreadyExistsException(message: String) : ApiException(message)

class BadRequestException(message: String) : ApiException(message)

class ConflictException(message: String) : ApiException(message)

class AuthenticationException(message: String) : ApiException(message)

class ValidationException(message: String) : ApiException(message)
