package com.example.androidproject1.feature.orders.di

import androidx.room.Room
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.feature.orders.data.database.OrdersDatabase
import com.example.androidproject1.feature.orders.data.repository.DefaultOrdersEraser
import com.example.androidproject1.feature.orders.data.repository.DefaultOrdersRepository
import com.example.androidproject1.feature.orders.data.source.DefaultLocalOrdersDataSource
import com.example.androidproject1.feature.orders.data.source.DefaultRemoteOrdersDataSource
import com.example.androidproject1.feature.orders.data.source.LocalOrdersDataSource
import com.example.androidproject1.feature.orders.data.source.RemoteOrdersDataSource
import com.example.androidproject1.feature.orders.domain.OrdersRepository
import com.example.androidproject1.feature.orders.presentation.orderdetail.OrderDetailViewModel
import com.example.androidproject1.feature.orders.presentation.orders.OrdersViewModel
import com.example.androidproject1.feature.orders.presentation.ordersDoor
import com.example.androidproject1.service.core.domain.LocalDataEraser
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

object OrdersModule {

    val module: Module = module {
        // Its door in the shell: Home, the drawer and Profile read every door with getAll().
        single<ShellDoor>(named("door:orders")) { ordersDoor(get()) }
        viewModelOf(::OrdersViewModel)
        viewModelOf(::OrderDetailViewModel)

        // Its own database: an order outlives the cart it came from and every catalog refresh.
        single {
            Room.databaseBuilder(androidContext(), OrdersDatabase::class.java, OrdersDatabase.NAME)
                .addMigrations(*OrdersDatabase.MIGRATIONS)
                .build()
        }
        single { get<OrdersDatabase>().ordersDao() }

        singleOf(::DefaultOrdersRepository) bind OrdersRepository::class
        singleOf(::DefaultLocalOrdersDataSource) bind LocalOrdersDataSource::class
        singleOf(::DefaultRemoteOrdersDataSource) bind RemoteOrdersDataSource::class
        // What "delete my account" wipes here.
        singleOf(::DefaultOrdersEraser) bind LocalDataEraser::class
    }
}
