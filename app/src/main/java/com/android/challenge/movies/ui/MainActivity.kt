package com.android.challenge.movies.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.res.Resources
import android.database.MatrixCursor
import android.graphics.Point
import android.os.Bundle
import android.support.v4.widget.CursorAdapter
import android.support.v4.widget.SimpleCursorAdapter
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.android.challenge.movies.R
import com.android.challenge.movies.repository.MoviesGridAdapter
import com.android.challenge.movies.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private val imageOptimalWidthDp = 100
    private val imageOptimalRatio = 16f/9f

    private lateinit var searchViewAdapter: SimpleCursorAdapter

    private lateinit var viewModel: MainViewModel

    private lateinit var searchViewItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initSearchAdapter()

        val columnSpan = calculateColumnSpan()
        val viewAdapter = MoviesGridAdapter(this, calculatePreferredHeight(columnSpan), 10) {
            viewModel.getNextPage(it)
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
        viewModel.suggestionsStream.observe(this, Observer {
            searchViewAdapter.changeCursor(it)
        })

        if (savedInstanceState == null)
            startNowPlayingMoviesSearch()
    }

    private fun initSearchAdapter() {
        searchViewAdapter = SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1,
                null,
                arrayOf("title"),
                intArrayOf(android.R.id.text1),
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main_menu, menu)

        searchViewItem = menu.findItem(R.id.search)
        val actionView = searchViewItem.actionView as SearchView
        actionView.queryHint = getString(R.string.hint_query)
        actionView.suggestionsAdapter = searchViewAdapter

        actionView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { startMoviesSearch(it) }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                query?.let { viewModel.newSuggestionsQuery(it) }
                return true
            }
        })

        actionView.setOnSuggestionListener(object: SearchView.OnSuggestionListener {

            override fun onSuggestionSelect(position: Int): Boolean { return false }

            override fun onSuggestionClick(position: Int): Boolean {
                (searchViewAdapter.getItem(position) as? MatrixCursor)?.let { matrix ->
                    matrix.getString(matrix.getColumnIndex("title"))?.let {
                        startMoviesSearch(it)
                        return true
                    }
                }
                return false
            }
        })

        return true
    }

    private fun startMoviesSearch(query: String) {
        viewModel.startMoviesSearch(query)
        searchViewItem.collapseActionView()
        supportActionBar?.subtitle = getString(R.string.search_query_subtitle_stub, query)
    }

    private fun startNowPlayingMoviesSearch() {
        viewModel.startNowPlayingMoviesSearch()
        supportActionBar?.subtitle = getString(R.string.now_playing_subtitle)
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