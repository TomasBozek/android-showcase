package com.example.androidproject1.feature.movies.data.source

import com.example.androidproject1.feature.movies.domain.MovieDetail
import com.example.androidproject1.feature.movies.domain.MoviePage
import com.example.androidproject1.feature.movies.domain.TvMazeConfig
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import com.example.androidproject1.service.network.HttpErrorMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.withContext

/**
 * TVmaze over the shared client. The URLs are absolute: the client's `defaultRequest`
 * names the template's own `BASE_URL`, and Ktor leaves a request that brings its own host alone, so
 * one client serves both servers. TVmaze takes no key.
 *
 * Switching to IO is this class's job, not the repository's: `BaseRepository` runs on the caller's
 * context, and the caller is `viewModelScope` — which is `Dispatchers.Main`. Failures are mapped
 * here so the repository above sees a `DomainError` and never an HTTP status.
 */
class DefaultRemoteMoviesDataSource(
    private val client: HttpClient,
    private val dispatcherProvider: DispatcherProvider,
    private val config: TvMazeConfig,
) : RemoteMoviesDataSource {

    override suspend fun getPopular(page: Int): MoviePage = request {
        try {
            val shows = client.get(config.apiBaseUrl + "shows") {
                parameter("page", page - 1)
            }.body<List<ShowDto>>()
            MoviePage(page = page, movies = shows.map { it.toDomain() }, isLast = shows.isEmpty())
        } catch (pastTheEnd: ClientRequestException) {
            if (pastTheEnd.response.status != HttpStatusCode.NotFound) throw pastTheEnd
            MoviePage(page = page, movies = emptyList(), isLast = true)
        }
    }

    override suspend fun getMovie(id: Int): MovieDetail = request {
        client.get(config.apiBaseUrl + "shows/$id").body<ShowDto>().toDetail()
    }

    private suspend fun <T> request(block: suspend () -> T): T = withContext(dispatcherProvider.io) {
        try {
            block()
        } catch (throwable: Throwable) {
            throw HttpErrorMapper.map(throwable)
        }
    }
}
