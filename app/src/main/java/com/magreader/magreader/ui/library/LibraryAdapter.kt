package com.magreader.magreader.ui.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.magreader.magreader.data.OpdsEntry
import com.magreader.magreader.data.OpdsManager
import com.magreader.magreader.databinding.ItemBookBinding
import okhttp3.Credentials

class LibraryAdapter(
    private val opdsManager: OpdsManager,
    private val onItemClick: (OpdsEntry) -> Unit
) : ListAdapter<OpdsEntry, LibraryAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookViewHolder(private val binding: ItemBookBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: OpdsEntry) {
            binding.textTitle.text = entry.title
            
            if (entry.thumbnailUrl != null) {
                binding.imageThumbnail.load(entry.thumbnailUrl) {
                    crossfade(true)
                    val user = opdsManager.username
                    val pass = opdsManager.password
                    if (!user.isNullOrEmpty() && !pass.isNullOrEmpty()) {
                        addHeader("Authorization", Credentials.basic(user, pass))
                    }
                }
            } else {
                binding.imageThumbnail.setImageDrawable(null)
            }

            binding.root.setOnClickListener { onItemClick(entry) }
        }
    }

    class BookDiffCallback : DiffUtil.ItemCallback<OpdsEntry>() {
        override fun areItemsTheSame(oldItem: OpdsEntry, newItem: OpdsEntry): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: OpdsEntry, newItem: OpdsEntry): Boolean = oldItem == newItem
    }
}
