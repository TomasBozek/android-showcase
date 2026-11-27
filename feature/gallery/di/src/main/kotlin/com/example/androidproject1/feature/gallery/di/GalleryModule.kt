package com.example.androidproject1.feature.gallery.di

import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.feature.gallery.presentation.gallery.GalleryViewModel
import com.example.androidproject1.feature.gallery.presentation.galleryDoor
import com.example.androidproject1.feature.gallery.presentation.galleryJumps
import com.example.androidproject1.feature.gallery.presentation.gallerydetail.GalleryDetailViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

object GalleryModule {

    val module: Module = module {
        // Its door in the shell: Home, the drawer and Profile read every door with getAll().
        single<ShellDoor>(named("door:gallery")) { galleryDoor() }
        // The screens the debug menu jumps to; read with getAll() where its modules are bound.
        galleryJumps.forEach { jump -> single<ShellJump>(named("jump:${jump.id}")) { jump } }
        viewModelOf(::GalleryViewModel)
        viewModelOf(::GalleryDetailViewModel)
    }
}
