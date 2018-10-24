package com.android.challenge.movies.model

import com.google.gson.annotations.SerializedName

data class Movie(
        @SerializedName("poster_path") val posterPath: String?,
        @SerializedName("backdrop_path") val backdropPath: String?,
        val overview: String)