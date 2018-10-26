package com.android.challenge.movies.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import com.android.challenge.movies.model.Movie
import com.android.challenge.movies.network.SearchResults
import com.android.challenge.movies.repository.MovieDbRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val moviesStream : MutableLiveData<List<Movie>> = MutableLiveData()

    private val fakePlaceholders = List(20) { Movie(null, null, String(), String()) }

    private var moviesCurrentPage = 0
    private val moviesData : MutableList<Movie> = mutableListOf()
    private val moviesDbRepository = MovieDbRepository()

    private var state: MainViewModelState = IdleState

    fun startNowPlayingMoviesSearch() {
        state = NowPlayingMoviesState
        moviesCurrentPage = 1
        moviesData.clear()
        getNowPlayingMovies()
    }

    fun getNextPage() {
        if (state === NowPlayingMoviesState) {
            moviesCurrentPage++
            getNowPlayingMovies()
        }
    }

    private fun getNowPlayingMovies() {
        moviesStream.postValue(moviesData + fakePlaceholders)

        moviesDbRepository.getNowPlayingMovies(getApplication(), moviesCurrentPage).enqueue(object: Callback<SearchResults?> {
            override fun onFailure(call: Call<SearchResults?>?, t: Throwable?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onResponse(call: Call<SearchResults?>?, response: Response<SearchResults?>?) {
                response?.body()?.results?.let {
                    moviesData.addAll(it)
                    moviesStream.postValue(moviesData)
                }
            }
        })
    }

}