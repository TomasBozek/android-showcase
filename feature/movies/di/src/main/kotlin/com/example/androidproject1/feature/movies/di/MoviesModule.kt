package com.example.androidproject1.feature.movies.di

import androidx.room.Room
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.feature.movies.data.database.MoviesDatabase
import com.example.androidproject1.feature.movies.data.repository.DefaultMovieFavouritesRepository
import com.example.androidproject1.feature.movies.data.repository.DefaultMoviesEraser
import com.example.androidproject1.feature.movies.data.repository.DefaultMoviesRepository
import com.example.androidproject1.feature.movies.data.source.DefaultLocalMovieFavouritesDataSource
import com.example.androidproject1.feature.movies.data.source.DefaultLocalMoviesDataSource
import com.example.androidproject1.feature.movies.data.source.DefaultRemoteMoviesDataSource
import com.example.androidproject1.feature.movies.data.source.LocalMovieFavouritesDataSource
import com.example.androidproject1.feature.movies.data.source.LocalMoviesDataSource
import com.example.androidproject1.feature.movies.data.source.RemoteMoviesDataSource
import com.example.androidproject1.feature.movies.domain.MovieFavouritesRepository
import com.example.androidproject1.feature.movies.domain.MoviesRepository
import com.example.androidproject1.feature.movies.domain.TvMazeConfig
import com.example.androidproject1.feature.movies.presentation.moviedetail.MovieDetailViewModel
import com.example.androidproject1.feature.movies.presentation.moviefavourites.MovieFavouritesViewModel
import com.example.androidproject1.feature.movies.presentation.movies.MoviesViewModel
import com.example.androidproject1.feature.movies.presentation.moviesDoor
import com.example.androidproject1.feature.movies.presentation.moviesJumps
import com.example.androidproject1.service.core.domain.LocalDataEraser
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

object MoviesModule {

    val module: Module = module {
        // Its door in the shell: Home, the drawer and Profile read every door with getAll().
        single<ShellDoor>(named("door:movies")) { moviesDoor(get()) }
        // The screens the debug menu jumps to; read with getAll() where its modules are bound.
        moviesJumps.forEach { jump -> single<ShellJump>(named("jump:${jump.id}")) { jump } }
        viewModelOf(::MoviesViewModel)
        viewModelOf(::MovieDetailViewModel)
        viewModelOf(::MovieFavouritesViewModel)

        single {
            Room.databaseBuilder(
                androidContext(),
                MoviesDatabase::class.java,
                MoviesDatabase.NAME,
            ).addMigrations(*MoviesDatabase.MIGRATIONS)
                .build()
        }
        single { get<MoviesDatabase>().moviesDao() }
        single { get<MoviesDatabase>().movieFavouritesDao() }

        singleOf(::DefaultMoviesRepository) bind MoviesRepository::class
        // TVmaze's host, the same on every flavor, so the feature's own.
        single { TvMazeConfig.DEFAULT }
        singleOf(::DefaultRemoteMoviesDataSource) bind RemoteMoviesDataSource::class
        singleOf(::DefaultMovieFavouritesRepository) bind MovieFavouritesRepository::class
        // Spelled out for the same reason as the movies source: a defaulted clock.
        single<LocalMovieFavouritesDataSource> { DefaultLocalMovieFavouritesDataSource(movieFavouritesDao = get()) }
        // Spelled out, like the catalog's: the data source has a defaulted clock for the page
        // markers, and reflection would try to resolve the `() -> Long` from the graph.
        single<LocalMoviesDataSource> { DefaultLocalMoviesDataSource(moviesDao = get()) }
        // What "delete my account" wipes here; the auth feature collects every one with getAll().
        singleOf(::DefaultMoviesEraser) bind LocalDataEraser::class
    }
}
