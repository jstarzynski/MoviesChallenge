package com.android.challenge.movies.ui

import android.animation.ArgbEvaluator
import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.android.challenge.movies.R
import com.android.challenge.movies.model.Movie
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions

class MoviesGridAdapter(private val activity: Activity,
                        private val preferredHeight: Int,
                        private val reachingEndOffset: Int,
                        private val onReachingEndListener: () -> Unit)
    : RecyclerView.Adapter<MoviesGridAdapter.MovieItemViewHolder>() {

    private val placeholderColors: IntArray = IntArray(13)

    init {
        val evaluator = ArgbEvaluator()
        val color1 = ContextCompat.getColor(activity, R.color.colorPlaceholder1)
        val color2 = ContextCompat.getColor(activity, R.color.colorPlaceholder2)
        val color3 = ContextCompat.getColor(activity, R.color.colorPlaceholder3)

        for (i in 0..12) when {
            i < 6 -> placeholderColors[i] = evaluator.evaluate(i / 6f, color1, color2) as Int
            else -> placeholderColors[i] = evaluator.evaluate((i - 6) / 6f, color2, color3) as Int
        }
    }

    inner class MovieItemViewHolder(movieItem: View) : RecyclerView.ViewHolder(movieItem) {
        val title: TextView = movieItem.findViewById(R.id.title)
        val image: ImageView = movieItem.findViewById(R.id.image)
    }

    private var moviesList : List<List<Movie>> = listOf()
    private var onClickListener: ((Movie) -> Unit)? = null

    fun onItemSelected(listener: (Movie) -> Unit) {
        onClickListener = listener
    }

    fun updateMoviesList(moviesList: List<List<Movie>>) {
        this.moviesList = moviesList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieItemViewHolder {
        val viewHolder = MovieItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.movie_list_item, parent, false))
        viewHolder.itemView.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, preferredHeight)
        return viewHolder
    }


    override fun getItemCount(): Int = moviesList.a.size

    override fun onBindViewHolder(holder: MovieItemViewHolder, position: Int) {

        if (itemCount - position == reachingEndOffset)
            onReachingEndListener()

        if (moviesList[position].title.isNotEmpty())
            holder.itemView.setOnClickListener { onClickListener?.let { it(moviesList[position]) } }
        else
            holder.itemView.setOnClickListener(null)

        if (moviesList[position].posterPath != null) {
            holder.image.visibility = View.VISIBLE
            holder.title.visibility = View.GONE
            holder.itemView.setBackgroundColor(0)

            Glide.with(activity)
                    .load(moviesList[position].posterPath?.let { "http://image.tmdb.org/t/p/w500$it" } ?: ColorDrawable(placeholderColors[position % placeholderColors.size]))
                    .apply(RequestOptions.centerCropTransform().placeholder(ColorDrawable(placeholderColors[position % placeholderColors.size])))
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(holder.image)
        } else {
            holder.image.visibility = View.GONE
            holder.title.visibility = View.VISIBLE
            holder.itemView.setBackgroundColor(placeholderColors[position % placeholderColors.size])

            holder.title.text = moviesList[position].title
        }
    }

    override fun onViewRecycled(holder: MovieItemViewHolder) {
        Glide.with(activity).clear(holder.image)
    }
}