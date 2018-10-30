package com.android.challenge.movies.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.database.AbstractCursor
import android.database.MatrixCursor
import android.provider.BaseColumns
import android.util.Log
import com.android.challenge.movies.model.Movie
import com.android.challenge.movies.network.SearchResults
import com.android.challenge.movies.repository.MovieDbRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val moviesStream : MutableLiveData<List<List<Movie>>> = MutableLiveData()
    val suggestionsStream: MutableLiveData<AbstractCursor> = MutableLiveData()
    val errorStream: MutableLiveData<Throwable> = MutableLiveData()

    private val fakePlaceholders = List(20) { Movie.PLACEHOLDER }

    private var moviesCurrentPage = 0
    private var moviesEndOfData: Boolean = false
    private val moviesData : MutableList<List<Movie>> = mutableListOf()
    private val moviesDbRepository = MovieDbRepository()

    private val suggestionsQueryExecutor = Executors.newSingleThreadExecutor()
    private val suggestionsQueryRunning = AtomicBoolean(false)
    private var nextSuggestionQuery: String? = null

    private var state: MainViewModelState = IdleState

    fun startNowPlayingMoviesSearch() {
        state = NowPlayingMoviesState
        moviesCurrentPage = 1
        moviesData.clear()
        moviesEndOfData = false
        getNowPlayingMovies()
    }

    fun startMoviesSearch(query: String) {
        state = SearchingMoviesState(query)
        moviesCurrentPage = 1
        moviesData.clear()
        moviesEndOfData = false
        getSearchedMovies(query)
    }

    fun getNextPage() {
        if (!moviesEndOfData) {
            moviesCurrentPage++
            when {
                state === NowPlayingMoviesState -> getNowPlayingMovies()
                else -> (state as? SearchingMoviesState)?.let { getSearchedMovies(it.searchQuery) }
            }
        }
    }

    fun newSuggestionsQuery(query: String) {
        suggestionsQueryExecutor.execute {
            if (suggestionsQueryRunning.get())
                nextSuggestionQuery = query
            else
                queryMoviesSuggestions(query)
        }
    }

    private fun getNowPlayingMovies() {
        moviesData.add(moviesCurrentPage - 1, fakePlaceholders)
        moviesStream.postValue(moviesData)
        moviesDbRepository.getNowPlayingMovies(getApplication(), moviesCurrentPage).enqueue(searchCallback)
    }

    private fun getSearchedMovies(query: String) {
        moviesData.add(moviesCurrentPage - 1, fakePlaceholders)
        moviesStream.postValue(moviesData)
        moviesDbRepository.searchMovies(getApplication(), query, moviesCurrentPage).enqueue(searchCallback)
    }

    private val searchCallback = object: Callback<SearchResults?> {
        override fun onFailure(call: Call<SearchResults?>?, t: Throwable?) {
            errorStream.postValue(t)
        }

        override fun onResponse(call: Call<SearchResults?>?, response: Response<SearchResults?>?) {
            response?.body()?.let { if (it.page == it.totalPages) moviesEndOfData = true }
            Log.v("searchMovies", "successResponse: ${response?.body()?.page}")
            response?.body()?.let {
                moviesData.add(it.page - 1, it.results)
                moviesStream.postValue(moviesData)
            } ?: Log.v("searchMovies", "errorResponse: ${response?.errorBody()}")
        }
    }

    private fun queryMoviesSuggestions(query: String) {
        suggestionsQueryExecutor.execute {
            suggestionsQueryRunning.set(true)
            moviesDbRepository.searchMovies(getApplication(), query, 1).enqueue(object: Callback<SearchResults?> {
                override fun onFailure(call: Call<SearchResults?>?, t: Throwable?) {
                    errorStream.postValue(t)
                    processNextSuggestion()
                }

                override fun onResponse(call: Call<SearchResults?>?, response: Response<SearchResults?>?) {
                    response?.body()?.results?.distinctBy { it.title }?.let {
                        val matrixCursor = MatrixCursor(arrayOf(BaseColumns._ID, SUGGESTIONS_TITLE_COLUMN_NAME))
                        it.forEachIndexed { index, movie -> matrixCursor.addRow(arrayOf(index, movie.title)) }
                        suggestionsStream.postValue(matrixCursor)
                    }
                    processNextSuggestion()
                }
            })
        }
    }

    private fun processNextSuggestion() {
        suggestionsQueryExecutor.execute {
            suggestionsQueryRunning.set(false)
            nextSuggestionQuery?.let {
                nextSuggestionQuery = null
                queryMoviesSuggestions(it)
            }
        }
    }

    companion object {
        const val SUGGESTIONS_TITLE_COLUMN_NAME = "title"
    }

}