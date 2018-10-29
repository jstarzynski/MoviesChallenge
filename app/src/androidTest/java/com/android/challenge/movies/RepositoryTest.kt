package com.android.challenge.movies

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.android.challenge.movies.model.Movie
import com.android.challenge.movies.repository.MovieDbRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class RepositoryTest {

    @Test
    fun useGetNowPlayingMovies() {
        val appContext = InstrumentationRegistry.getTargetContext()
        val repo = MovieDbRepository()
        val results: MutableList<Movie> = mutableListOf()

        for (index in 1..10)
            getNowPlayingPaginationStep(appContext, repo, index, results)
        assertEquals(20 * 100, results.size)
    }

    @Test
    fun useSearchMovies() {
        val appContext = InstrumentationRegistry.getTargetContext()
        val repo = MovieDbRepository()
        val response = repo.searhMovies(appContext, "crazy", 1).execute().body()
        assertNotNull(response)
    }

    private fun getNowPlayingPaginationStep(context: Context, repo: MovieDbRepository, stepIndex: Int, accumulateResults: MutableList<Movie>) {
        val response = repo.getNowPlayingMovies(context, stepIndex).execute().body()
        assertNotNull(response)
        accumulateResults.addAll(response?.results ?: listOf())
    }
}
