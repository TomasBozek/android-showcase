package com.example.androidproject1.service.core.domain.error

/** The device could not reach the server at all. */
public class NetworkError(
    override val cause: Throwable? = null,
) : DomainError()

/** The server was reached but answered with a 5xx. */
public class ServerError(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : DomainError()

/** Credentials are missing or no longer valid. */
public class UnauthorizedError(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : DomainError()

/** The request was rejected as malformed. */
public class BadRequestError(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : DomainError()

/**
 * The server asked the app to slow down — a `429`. Not the user's mistake and not a broken
 * server: the same request will work in a while, so a screen words it as "wait", never as a fault.
 */
public class RateLimitedError(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : DomainError()

/** The requested resource does not exist. */
public class NotFoundError(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : DomainError()

/** Anything unforeseen — a bug rather than a condition worth modelling. */
public class UnexpectedError(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : DomainError()
