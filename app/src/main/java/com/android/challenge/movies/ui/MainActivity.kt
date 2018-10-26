package com.android.challenge.movies.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.res.Resources
import android.graphics.Point
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.view.View
import com.android.challenge.movies.R
import com.android.challenge.movies.repository.MoviesGridAdapter
import com.android.challenge.movies.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private val imageOptimalWidthDp = 100
    private val imageOptimalRatio = 16f/9f

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val columnSpan = calculateColumnSpan()
        val viewAdapter = MoviesGridAdapter(this, calculatePreferredHeight(columnSpan), 10) {
            viewModel.getNextPage()
        }

        viewAdapter.onItemSelected {movie ->
            startActivity(Intent(this, DetailActivity::class.java).also {
                it.putExtra(DetailActivity.EXTRA_BACKDROP, movie.backdropPath)
                it.putExtra(DetailActivity.EXTRA_OVERVIEW, movie.overview)
                it.putExtra(DetailActivity.EXTRA_TITLE, movie.title)
            })
        }

        moviesGrid.apply {
            layoutManager = GridLayoutManager(context, columnSpan)
            adapter = viewAdapter
        }

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel.moviesStream.observe(this, Observer {
            viewAdapter.updateMoviesList(it ?: listOf())
            if (it != null) {
                moviesGrid.visibility = View.VISIBLE
                progress.visibility = View.GONE
            }
        })

        if (savedInstanceState == null)
            viewModel.startNowPlayingMoviesSearch()
    }

    private fun calculateColumnSpan() = Point().run {
        windowManager.defaultDisplay.getSize(this)
        x / (imageOptimalWidthDp * Resources.getSystem().displayMetrics.density).roundToInt()
    }

    private fun calculatePreferredHeight(columnSpan: Int) = Point().run {
        windowManager.defaultDisplay.getSize(this)
        (imageOptimalRatio * x / columnSpan).toInt()
    }

}