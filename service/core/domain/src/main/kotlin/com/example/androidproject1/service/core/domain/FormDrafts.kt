package com.example.androidproject1.service.core.domain

/**
 * Where a form's unfinished input waits between launches: a value under a key, the form's
 * own serialised draft. A form writes on every edit, reads when it opens and clears when it is
 * saved or discarded; a password is never put here.
 *
 * Bound once in `coreModule`, and a [LocalDataEraser] too — a draft is account data, so a sign-out
 * takes every one of them.
 */
public interface FormDrafts {

    /** The draft saved under [key], or `null` when there is none. */
    public suspend fun read(key: String): String?

    public suspend fun write(key: String, value: String)

    public suspend fun clear(key: String)
}
