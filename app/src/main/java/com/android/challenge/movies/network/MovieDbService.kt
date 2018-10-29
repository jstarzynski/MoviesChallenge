package com.android.challenge.movies.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MovieDbService {

    @GET("/3/movie/now_playing")
    fun getNowPlayingMovies(@Query("api_key") apiKey: String, @Query("page") page: Int): Call<SearchResults>

    @GET("/3/search/movie")
    fun searchMovies(@Query("api_key") apiKey: String,
                     @Query("query") query: String,
                     @Query("page") page: Int): Call<SearchResults>
}