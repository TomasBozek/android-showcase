package com.example.androidproject1.feature.weather.di

import androidx.room.Room
import com.example.androidproject1.core.ui.shell.ShellCreation
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.feature.weather.data.database.WeatherDatabase
import com.example.androidproject1.feature.weather.data.repository.DefaultWeatherEraser
import com.example.androidproject1.feature.weather.data.repository.DefaultWeatherRepository
import com.example.androidproject1.feature.weather.data.repository.DefaultWeatherUnitsRepository
import com.example.androidproject1.feature.weather.data.source.DefaultLocalWeatherDataSource
import com.example.androidproject1.feature.weather.data.source.DefaultLocalWeatherUnitsDataSource
import com.example.androidproject1.feature.weather.data.source.DefaultRemoteWeatherDataSource
import com.example.androidproject1.feature.weather.data.source.DefaultSuggestedPlacesDataSource
import com.example.androidproject1.feature.weather.data.source.LocalWeatherDataSource
import com.example.androidproject1.feature.weather.data.source.LocalWeatherUnitsDataSource
import com.example.androidproject1.feature.weather.data.source.RemoteWeatherDataSource
import com.example.androidproject1.feature.weather.data.source.SuggestedPlacesDataSource
import com.example.androidproject1.feature.weather.domain.OpenMeteoConfig
import com.example.androidproject1.feature.weather.domain.WeatherRepository
import com.example.androidproject1.feature.weather.domain.WeatherUnitsRepository
import com.example.androidproject1.feature.weather.presentation.R
import com.example.androidproject1.feature.weather.presentation.forecast.ForecastViewModel
import com.example.androidproject1.feature.weather.presentation.placeCreation
import com.example.androidproject1.feature.weather.presentation.weather.PlaceNames
import com.example.androidproject1.feature.weather.presentation.weather.WeatherViewModel
import com.example.androidproject1.feature.weather.presentation.weatherDoor
import com.example.androidproject1.feature.weather.presentation.weatherJumps
import com.example.androidproject1.service.core.domain.LocalDataEraser
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

object WeatherModule {

    val module: Module = module {
        // Its door in the shell: Home, the drawer and Profile read every door with getAll().
        single<ShellDoor>(named("door:weather")) { weatherDoor(get()) }
        single<ShellCreation>(named("create:place")) { placeCreation() }
        // The screens the debug menu jumps to; read with getAll() where its modules are bound.
        weatherJumps.forEach { jump -> single<ShellJump>(named("jump:${jump.id}")) { jump } }
        viewModelOf(::WeatherViewModel)
        // The name of a place the geocoder could not name, resolved when it is saved.
        single<PlaceNames> {
            val context = androidContext()
            PlaceNames { context.getString(R.string.weather_my_location) }
        }
        viewModelOf(::ForecastViewModel)

        single {
            Room.databaseBuilder(
                androidContext(),
                WeatherDatabase::class.java,
                WeatherDatabase.NAME,
            ).addMigrations(*WeatherDatabase.MIGRATIONS)
                .build()
        }
        single { get<WeatherDatabase>().weatherDao() }

        singleOf(::DefaultWeatherRepository) bind WeatherRepository::class
        singleOf(::DefaultRemoteWeatherDataSource) bind RemoteWeatherDataSource::class
        // Open-Meteo's two hosts, the same on every flavor, so the feature's own.
        single { OpenMeteoConfig.DEFAULT }
        // Spelled out, like the movies': the data source has a defaulted clock, and reflection
        // would try to resolve the `() -> Long` from the graph.
        single<LocalWeatherDataSource> { DefaultLocalWeatherDataSource(weatherDao = get()) }
        // What "delete my account" wipes here; the auth feature collects every one with getAll().
        singleOf(::DefaultWeatherEraser) bind LocalDataEraser::class
        singleOf(::DefaultWeatherUnitsRepository) bind WeatherUnitsRepository::class
        singleOf(::DefaultLocalWeatherUnitsDataSource) bind LocalWeatherUnitsDataSource::class
        singleOf(::DefaultSuggestedPlacesDataSource) bind SuggestedPlacesDataSource::class
    }
}
