package com.android.challenge.movies.viewmodel

sealed class MainViewModelState

object IdleState : MainViewModelState()
object NowPlayingMoviesState : MainViewModelState()
data class SearchingMoviesState(val searchQuery: String) : MainViewModelState()