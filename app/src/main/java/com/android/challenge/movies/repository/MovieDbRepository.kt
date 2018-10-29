package com.android.challenge.movies.repository

import android.content.Context
import com.android.challenge.movies.R
import com.android.challenge.movies.network.MovieDbService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MovieDbRepository {

    private val movieDbService: MovieDbService = Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MovieDbService::class.java)


    fun getNowPlayingMovies(context: Context, page: Int) =
            movieDbService.getNowPlayingMovies(context.getString(R.string.MOVIE_DB_API_KEY), page)

    fun searhMovies(context: Context, query: String, page: Int) =
            movieDbService.searchMovies(context.getString(R.string.MOVIE_DB_API_KEY), query, page)

}