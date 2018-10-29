package com.android.challenge.movies.network

import com.android.challenge.movies.model.Movie
import com.google.gson.annotations.SerializedName

data class SearchResults(
        val page: Int,
        @SerializedName("total_pages") val totalPages: Int,
        val results: List<Movie>)