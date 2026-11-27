package com.example.androidproject1.service.log.domain

/**
 * Minimal logging abstraction, kept free of `android.*` so this module stays the one layer that
 * knows nothing about the platform. Implementations live a layer down, in `:service:log:data`.
 */
public interface Logger {

    public fun withTag(tag: String): Logger

    public fun d(throwable: Throwable? = null, message: () -> String = { "" })

    public fun w(throwable: Throwable? = null, message: () -> String = { "" })

    public fun e(throwable: Throwable? = null, message: () -> String = { "" })
}
