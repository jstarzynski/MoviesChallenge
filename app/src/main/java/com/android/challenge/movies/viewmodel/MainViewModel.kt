package com.android.challenge.movies.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.database.AbstractCursor
import android.database.MatrixCursor
import android.provider.BaseColumns
import com.android.challenge.movies.model.Movie
import com.android.challenge.movies.network.SearchResults
import com.android.challenge.movies.repository.MovieDbRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.locks.ReentrantLock

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val moviesStream : MutableLiveData<List<Movie>> = MutableLiveData()
    val suggestionsStream: MutableLiveData<AbstractCursor> = MutableLiveData()

    private val fakePlaceholders = List(20) { Movie(null, null, String(), String()) }

    private var moviesCurrentPage = 0
    private var moviesEndOfData: Boolean = false
    private val moviesData : MutableList<Movie> = mutableListOf()
    private val moviesDbRepository = MovieDbRepository()

    private val suggestionsQueryLock = ReentrantLock()
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
        moviesEndOfData = false;
        getSearchedMovies(query)
    }

    fun getNextPage(numberOfResultsLoaded: Int) {
        if (!moviesEndOfData && numberOfResultsLoaded <= moviesData.size) {
            moviesCurrentPage++
            when {
                state === NowPlayingMoviesState -> getNowPlayingMovies()
                else -> (state as? SearchingMoviesState)?.let { getSearchedMovies(it.searchQuery) }
            }
        }
    }

    fun newSuggestionsQuery(query: String) {
        if (!suggestionsQueryLock.isLocked)
            queryMoviesSuggestions(query)
        else
            nextSuggestionQuery = query
    }

    private fun getNowPlayingMovies() {
        moviesStream.postValue(moviesData + fakePlaceholders)

        moviesDbRepository.getNowPlayingMovies(getApplication(), moviesCurrentPage).enqueue(object: Callback<SearchResults?> {
            override fun onFailure(call: Call<SearchResults?>?, t: Throwable?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onResponse(call: Call<SearchResults?>?, response: Response<SearchResults?>?) {
                response?.body()?.let { if (it.page == it.totalPages) moviesEndOfData = true }
                response?.body()?.results?.let {
                    moviesData.addAll(it)
                    moviesStream.postValue(moviesData)
                }
            }
        })
    }

    private fun getSearchedMovies(query: String) {
        moviesStream.postValue(moviesData + fakePlaceholders)

        moviesDbRepository.searhMovies(getApplication(), query, moviesCurrentPage).enqueue(object: Callback<SearchResults?> {
            override fun onFailure(call: Call<SearchResults?>?, t: Throwable?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onResponse(call: Call<SearchResults?>?, response: Response<SearchResults?>?) {
                response?.body()?.let { if (it.page == it.totalPages) moviesEndOfData = true }
                response?.body()?.results?.let {
                    moviesData.addAll(it)
                    moviesStream.postValue(moviesData)
                }
            }
        })
    }

    private fun queryMoviesSuggestions(query: String) {

        suggestionsQueryLock.lock()
        moviesDbRepository.searhMovies(getApplication(), query, 1).enqueue(object: Callback<SearchResults?> {
            override fun onFailure(call: Call<SearchResults?>?, t: Throwable?) {
                processNextSuggestion()
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onResponse(call: Call<SearchResults?>?, response: Response<SearchResults?>?) {
                response?.body()?.results?.distinctBy { it.title }?.let {
                    val matrixCursor = MatrixCursor(arrayOf(BaseColumns._ID, "title"))
                    it.forEachIndexed { index, movie -> matrixCursor.addRow(arrayOf(index, movie.title)) }
                    suggestionsStream.postValue(matrixCursor)
                }
                processNextSuggestion()
            }
        })

    }

    private fun processNextSuggestion() {
        nextSuggestionQuery?.let {
            nextSuggestionQuery = null
            queryMoviesSuggestions(it)
        }
        suggestionsQueryLock.unlock()
    }

}