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
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val moviesStream : MutableLiveData<List<Movie>> = MutableLiveData()
    val suggestionsStream: MutableLiveData<AbstractCursor> = MutableLiveData()
    val errorStream: MutableLiveData<Throwable> = MutableLiveData()

    private var moviesCurrentPage = 0
    private var moviesEndOfData: Boolean = false
    private val moviesData : MutableMap<Int, List<Movie>> = mutableMapOf()
    private val moviesDbRepository = MovieDbRepository()

    private val suggestionsQueryExecutor = Executors.newSingleThreadExecutor()
    private val suggestionsQueryRunning = AtomicBoolean(false)
    private var nextSuggestionQuery: String? = null

    private val currentCalls = mutableSetOf<Call<SearchResults>>()

    private var state: MainViewModelState = IdleState

    fun startNowPlayingMoviesSearch() {
        cancelCurrentCalls()
        state = NowPlayingMoviesState
        moviesCurrentPage = 1
        moviesData.clear()
        moviesEndOfData = false
        getNowPlayingMovies(moviesCurrentPage)
    }

    fun startMoviesSearch(query: String) {
        cancelCurrentCalls()
        state = SearchingMoviesState(query)
        moviesCurrentPage = 1
        moviesData.clear()
        moviesEndOfData = false
        getSearchedMovies(moviesCurrentPage, query)
    }

    fun getNextPage() {
        if (!moviesEndOfData) {
            moviesCurrentPage++
            getPage(moviesCurrentPage)
        }
    }

    private fun getPage(page: Int) = when (state) {
        NowPlayingMoviesState -> getNowPlayingMovies(page)
        else -> (state as? SearchingMoviesState)?.let { getSearchedMovies(page, it.searchQuery) }
    }

    fun newSuggestionsQuery(query: String) {
        suggestionsQueryExecutor.execute {
            if (suggestionsQueryRunning.get())
                nextSuggestionQuery = query
            else
                queryMoviesSuggestions(query)
        }
    }

    fun isEndReached() = moviesEndOfData

    private fun getNowPlayingMovies(page: Int) {
        moviesDbRepository.getNowPlayingMovies(getApplication(), page).registerCall().enqueue(searchCallback)
    }

    private fun getSearchedMovies(page: Int, query: String) {
        moviesDbRepository.searchMovies(getApplication(), query, page).registerCall().enqueue(searchCallback)
    }

    private fun requestMissedPages() {
        val pagesLimit = moviesData.keys.sortedDescending().first()
        for (i in 0..pagesLimit)
            if (!moviesData.containsKey(i)) getPage(i)
    }

    private val searchCallback = object: Callback<SearchResults> {
        override fun onFailure(call: Call<SearchResults>?, t: Throwable?) {
            call?.unregisterCall()
            if (call?.isCanceled == false)
                errorStream.postValue(t)
        }

        override fun onResponse(call: Call<SearchResults>?, response: Response<SearchResults>?) {
            call?.unregisterCall()
            response?.body()?.let { if (it.page == it.totalPages) moviesEndOfData = true }
            response?.body()?.let {
                moviesData[it.page - 1] = it.results
                postSearchResults()
            }
            TODO("wrong behaviour below")
            requestMissedPages()
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

    private fun postSearchResults() {
        val postData: MutableList<Movie> = mutableListOf()
        val pagesLimit = moviesData.keys.sortedDescending().first()
        for (i in 0..pagesLimit)
            postData.addAll(moviesData[i] ?: List(MainViewModel.PLACEHOLDER_LIST_DEF_SIZE) { Movie.PLACEHOLDER })
        moviesStream.postValue(postData)
    }

    private fun cancelCurrentCalls() {
        currentCalls.removeAll {
            it.cancel()
            true
        }
    }

    private fun Call<SearchResults>.registerCall(): Call<SearchResults> {
        currentCalls.add(this)
        return this
    }

    private fun Call<SearchResults>.unregisterCall() = currentCalls.remove(this)

    companion object {
        const val SUGGESTIONS_TITLE_COLUMN_NAME = "title"
        const val PLACEHOLDER_LIST_DEF_SIZE = 20
    }

}