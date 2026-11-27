package com.example.androidproject1.service.core.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.example.androidproject1.service.core.domain.test.TestDispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Form drafts over the real preferences store. */
@RunWith(RobolectricTestRunner::class)
class DataStoreFormDraftsTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val provider = DataStoreProvider(context, name = "form_drafts_test_${System.nanoTime()}")
    private val drafts = DataStoreFormDrafts(provider, TestDispatchers)

    @Test
    fun `a draft written under a key reads back from a fresh instance`() = runTest {
        drafts.write("trip_wizard", "{\"name\":\"Lisbon\"}")

        assertEquals("{\"name\":\"Lisbon\"}", DataStoreFormDrafts(provider, TestDispatchers).read("trip_wizard"))
        assertNull(drafts.read("inventory_editor_new"))
    }

    @Test
    fun `clearing one key leaves the others`() = runTest {
        drafts.write("a", "1")
        drafts.write("b", "2")

        drafts.clear("a")

        assertNull(drafts.read("a"))
        assertEquals("2", drafts.read("b"))
    }

    @Test
    fun `erasing takes every draft and nothing else in the file`() = runTest {
        drafts.write("a", "1")
        drafts.write("b", "2")
        provider.dataStore.edit { it[stringPreferencesKey("theme")] = "dark" }

        drafts.erase()

        assertNull(drafts.read("a"))
        assertNull(drafts.read("b"))
        assertEquals("dark", provider.dataStore.data.first()[stringPreferencesKey("theme")])
    }
}
