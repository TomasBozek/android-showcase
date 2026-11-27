package com.example.androidproject1.service.core.domain.test

import com.example.androidproject1.service.core.domain.FormDrafts

/** In-memory [FormDrafts]: [saved] is the "disk", so a test can open a second form over it as a relaunch would. */
public class FakeFormDrafts(public val saved: MutableMap<String, String> = mutableMapOf()) : FormDrafts {

    override suspend fun read(key: String): String? = saved[key]

    override suspend fun write(key: String, value: String) {
        saved[key] = value
    }

    override suspend fun clear(key: String) {
        saved.remove(key)
    }
}
