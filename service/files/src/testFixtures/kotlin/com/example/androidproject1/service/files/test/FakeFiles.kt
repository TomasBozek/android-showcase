package com.example.androidproject1.service.files.test

import android.net.Uri
import com.example.androidproject1.service.files.FilePurpose
import com.example.androidproject1.service.files.FileStore
import java.io.File

/**
 * A [FileStore] over a directory the test hands it — `TemporaryFolder`'s root — so a test writes
 * real files and reads them back. [uriFor] answers a `content://fake.files/…` URI.
 */
class FakeFiles(private val root: File) : FileStore {

    override fun file(purpose: FilePurpose, name: String): File {
        require(name.isNotEmpty() && '/' !in name && name != "..") { "A file name is one path segment, not \"$name\"" }
        return File(File(root, purpose.name.lowercase()).apply { mkdirs() }, name)
    }

    override fun uriFor(file: File): Uri = Uri.parse("content://fake.files/${file.name}")

    override suspend fun delete(file: File): Boolean = !file.exists() || file.delete()
}
