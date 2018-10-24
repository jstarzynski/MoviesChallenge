package com.android.challenge.movies.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MovieDbService {

    @GET("/movie/now_playing")
    fun getNowPlayingMovies(@Query("api_key") apiKey: String, @Query("page") page: Int): Call<SearchResults>
}