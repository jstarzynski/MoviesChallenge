package com.android.challenge.movies.ui

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.android.challenge.movies.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.activity_details.*

class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_OVERVIEW = "EXTRA_OVERVIEW"
        const val EXTRA_BACKDROP = "EXTRA_BACKDROP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val backdropPath: String? = intent.extras.getString(EXTRA_BACKDROP)

        overview.text = intent.extras.getString(EXTRA_OVERVIEW)
        movieTitle.text = intent.extras.getString(EXTRA_TITLE)

        Glide.with(this)
                .load(backdropPath?.let { "http://image.tmdb.org/t/p/w500$it" } ?: ColorDrawable(ContextCompat.getColor(this,  R.color.colorPlaceholder1)))
                .apply(RequestOptions.centerCropTransform().placeholder(ColorDrawable(ContextCompat.getColor(this,  R.color.colorPlaceholder1))))
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .into(backdrop)
    }

    override fun onOptionsItemSelected(item: MenuItem?) = finish().let { true }

}