package com.example.androidproject1.feature.cart.di

import androidx.room.Room
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.feature.cart.data.database.CartDatabase
import com.example.androidproject1.feature.cart.data.repository.DefaultCartEraser
import com.example.androidproject1.feature.cart.data.repository.DefaultCartRepository
import com.example.androidproject1.feature.cart.data.source.DefaultLocalCartDataSource
import com.example.androidproject1.feature.cart.data.source.LocalCartDataSource
import com.example.androidproject1.feature.cart.domain.AddProductToCart
import com.example.androidproject1.feature.cart.domain.CartRepository
import com.example.androidproject1.feature.cart.presentation.cart.CartViewModel
import com.example.androidproject1.feature.cart.presentation.cartDoor
import com.example.androidproject1.service.core.domain.LocalDataEraser
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

object CartModule {

    val module: Module = module {
        // Its door in the shell: Home, the drawer and Profile read every door with getAll().
        single<ShellDoor>(named("door:cart")) { cartDoor(get()) }
        viewModelOf(::CartViewModel)

        // Product detail adds to the cart without depending on this feature's presentation.
        singleOf(::AddProductToCart)

        // Its own database, not the catalog\'s: feat.5 replaces the catalog table on every
        // refresh, and the cart must survive the shop reorganising itself.
        single {
            Room.databaseBuilder(androidContext(), CartDatabase::class.java, CartDatabase.NAME)
                .addMigrations(*CartDatabase.MIGRATIONS)
                .build()
        }
        single { get<CartDatabase>().cartDao() }

        singleOf(::DefaultCartRepository) bind CartRepository::class
        // Spelled out: the data source has a defaulted clock for tests, which reflection would
        // try to resolve from the graph.
        single<LocalCartDataSource> { DefaultLocalCartDataSource(cartDao = get()) }
        // What "delete my account" wipes here; the auth feature collects every one with getAll().
        singleOf(::DefaultCartEraser) bind LocalDataEraser::class
    }
}
