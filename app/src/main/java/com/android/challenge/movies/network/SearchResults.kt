package com.android.challenge.movies.network

import com.android.challenge.movies.model.Movie

data class SearchResults(
        val page: Int,
        val results: List<Movie>)