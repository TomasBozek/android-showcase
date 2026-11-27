package com.example.androidproject1.feature.trips.di

import androidx.room.Room
import com.example.androidproject1.core.ui.shell.ShellCreation
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.feature.trips.data.database.TripsDatabase
import com.example.androidproject1.feature.trips.data.reminder.DefaultTripReminders
import com.example.androidproject1.feature.trips.data.reminder.TripReminderHandler
import com.example.androidproject1.feature.trips.data.reminder.TripReminderWork
import com.example.androidproject1.feature.trips.data.repository.DefaultDestinationsRepository
import com.example.androidproject1.feature.trips.data.repository.DefaultTripsEraser
import com.example.androidproject1.feature.trips.data.repository.DefaultTripsRepository
import com.example.androidproject1.feature.trips.data.source.DefaultLocalDestinationsDataSource
import com.example.androidproject1.feature.trips.data.source.DefaultLocalTripsDataSource
import com.example.androidproject1.feature.trips.data.source.LocalDestinationsDataSource
import com.example.androidproject1.feature.trips.data.source.LocalTripsDataSource
import com.example.androidproject1.feature.trips.domain.DestinationsRepository
import com.example.androidproject1.feature.trips.domain.TripReminders
import com.example.androidproject1.feature.trips.domain.TripsRepository
import com.example.androidproject1.feature.trips.presentation.destinationpicker.DestinationPickerViewModel
import com.example.androidproject1.feature.trips.presentation.tripCreation
import com.example.androidproject1.feature.trips.presentation.tripdetail.TripDetailViewModel
import com.example.androidproject1.feature.trips.presentation.trips.TripsViewModel
import com.example.androidproject1.feature.trips.presentation.tripsDoor
import com.example.androidproject1.feature.trips.presentation.tripsJumps
import com.example.androidproject1.feature.trips.presentation.tripslist.TripsListViewModel
import com.example.androidproject1.feature.trips.presentation.tripwizard.TripWizardViewModel
import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.work.WorkHandler
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

object TripsModule {

    val module: Module = module {
        // Its door in the shell: Home, the drawer and Profile read every door with getAll().
        single<ShellDoor>(named("door:trips")) { tripsDoor(get()) }
        single<ShellCreation>(named("create:trip")) { tripCreation() }
        // The screens the debug menu jumps to; read with getAll() where its modules are bound.
        tripsJumps.forEach { jump -> single<ShellJump>(named("jump:${jump.id}")) { jump } }
        viewModelOf(::TripsViewModel)
        viewModelOf(::TripsListViewModel)
        viewModelOf(::TripWizardViewModel)
        viewModelOf(::TripDetailViewModel)
        viewModelOf(::DestinationPickerViewModel)

        single {
            Room.databaseBuilder(
                androidContext(),
                TripsDatabase::class.java,
                TripsDatabase.NAME,
            ).addMigrations(*TripsDatabase.MIGRATIONS)
                .build()
        }
        single { get<TripsDatabase>().tripDao() }
        single { get<TripsDatabase>().destinationDao() }

        singleOf(::DefaultTripsRepository) bind TripsRepository::class
        singleOf(::DefaultDestinationsRepository) bind DestinationsRepository::class

        // The reminder the evening before a trip: the store and the job kept together, what
        // runs when the job is due — found by the scheduler by its key — and the port the screen uses.
        singleOf(::TripReminderWork)
        singleOf(::TripReminderHandler) bind WorkHandler::class
        singleOf(::DefaultTripReminders) bind TripReminders::class
        single<LocalTripsDataSource> { DefaultLocalTripsDataSource(tripDao = get()) }
        single<LocalDestinationsDataSource> { DefaultLocalDestinationsDataSource(destinationDao = get()) }
        // What "delete my account" wipes here; the auth feature collects every one with getAll().
        singleOf(::DefaultTripsEraser) bind LocalDataEraser::class
    }
}
