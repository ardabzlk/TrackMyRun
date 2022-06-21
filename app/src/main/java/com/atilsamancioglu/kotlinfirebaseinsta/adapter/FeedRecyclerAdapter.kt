package com.example.trackmyrun.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trackmyrun.model.Post
import com.example.trackmyrun.R
import com.example.trackmyrun.databinding.RecyclerViewRowBinding
import com.squareup.picasso.Picasso

class FeedRecyclerAdapter(private val postList : ArrayList<Post>) : RecyclerView.Adapter<FeedRecyclerAdapter.PostHolder>() {

    class PostHolder(val binding: RecyclerViewRowBinding) : RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val binding = RecyclerViewRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return PostHolder(binding)
    }

    override fun getItemCount(): Int {
        return postList.size

    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        holder.binding.recyclerEmailText.text = postList.get(position).email
        holder.binding.recyclerCommentText.text = postList.get(position).comment
        holder.binding.recyclerDateText.text = postList.get(position).date.toString()
        holder.binding.recyclerDurationText.text = postList.get(position).routeDuration
        holder.binding.recyclerDistanceText.text = postList.get(position).routeDistance
        holder.binding.recyclerStartingLocationText.text = postList.get(position).startLocation
        holder.binding.recyclerEndingLocationText.text = postList.get(position).endLocation
        Picasso.get().load(postList[position].downloadUrl).into(holder.binding.recyclerImageView)
    }


}