package com.example.androidproject1.feature.inventory.di

import androidx.room.Room
import com.example.androidproject1.core.ui.shell.ShellCreation
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.feature.inventory.data.database.InventoryDatabase
import com.example.androidproject1.feature.inventory.data.repository.DefaultInventoryEraser
import com.example.androidproject1.feature.inventory.data.repository.DefaultInventoryRepository
import com.example.androidproject1.feature.inventory.data.source.DefaultLocalInventoryDataSource
import com.example.androidproject1.feature.inventory.data.source.DefaultRemoteInventoryDataSource
import com.example.androidproject1.feature.inventory.data.source.LocalInventoryDataSource
import com.example.androidproject1.feature.inventory.data.source.RemoteInventoryDataSource
import com.example.androidproject1.feature.inventory.data.sync.InventorySyncHandler
import com.example.androidproject1.feature.inventory.domain.InventoryRepository
import com.example.androidproject1.feature.inventory.presentation.inventory.InventoryViewModel
import com.example.androidproject1.feature.inventory.presentation.inventoryDoor
import com.example.androidproject1.feature.inventory.presentation.inventoryJumps
import com.example.androidproject1.feature.inventory.presentation.inventorydetail.InventoryDetailViewModel
import com.example.androidproject1.feature.inventory.presentation.inventoryeditor.InventoryEditorViewModel
import com.example.androidproject1.feature.inventory.presentation.itemCreation
import com.example.androidproject1.feature.inventory.presentation.message.InventoryRestoreHandler
import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.core.ui.event.MessageActionHandler
import com.example.androidproject1.service.work.WorkHandler
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

object InventoryModule {

    val module: Module = module {
        // Its door in the shell: Home, the drawer and Profile read every door with getAll().
        single<ShellDoor>(named("door:inventory")) { inventoryDoor(get()) }
        single<ShellCreation>(named("create:item")) { itemCreation() }
        // The screens the debug menu jumps to; read with getAll() where its modules are bound.
        inventoryJumps.forEach { jump -> single<ShellJump>(named("jump:${jump.id}")) { jump } }
        viewModelOf(::InventoryViewModel)
        viewModelOf(::InventoryEditorViewModel)
        viewModelOf(::InventoryDetailViewModel)

        single {
            Room.databaseBuilder(
                androidContext(),
                InventoryDatabase::class.java,
                InventoryDatabase.NAME,
            ).addMigrations(*InventoryDatabase.MIGRATIONS)
                .build()
        }
        single { get<InventoryDatabase>().itemDao() }
        single { get<InventoryDatabase>().outboxDao() }

        singleOf(::DefaultInventoryRepository) bind InventoryRepository::class
        // Spelled out: the source has a defaulted clock for the outbox's order.
        single<LocalInventoryDataSource> { DefaultLocalInventoryDataSource(itemDao = get(), outboxDao = get()) }
        // What "delete my account" wipes here; the auth feature collects every one with getAll().
        singleOf(::DefaultInventoryEraser) bind LocalDataEraser::class
        // The Undo on a deleted item's message, run after its screen has gone.
        singleOf(::InventoryRestoreHandler) bind MessageActionHandler::class
        singleOf(::DefaultRemoteInventoryDataSource) bind RemoteInventoryDataSource::class
        // Drains the outbox when WorkManager runs it; the app's WorkerFactory finds it by key.
        singleOf(::InventorySyncHandler) bind WorkHandler::class
    }
}
