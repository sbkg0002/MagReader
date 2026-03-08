package com.magreader.magreader.ui.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.ImageRequest
import com.magreader.magreader.data.Book
import com.magreader.magreader.data.KomgaManager
import com.magreader.magreader.databinding.ItemBookBinding
import okhttp3.Credentials

class LibraryAdapter(
    private val komgaManager: KomgaManager,
    private val onItemClick: (Book) -> Unit
) : ListAdapter<Book, LibraryAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookViewHolder(private val binding: ItemBookBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: Book) {
            binding.textTitle.text = book.metadata.title
            
            val url = "${komgaManager.serverUrl}/api/v1/books/${book.id}/thumbnail"
            
            binding.imageThumbnail.load(url) {
                crossfade(true)
                addHeader("Authorization", Credentials.basic(komgaManager.username ?: "", komgaManager.password ?: ""))
            }

            binding.root.setOnClickListener { onItemClick(book) }
        }
    }

    class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean = oldItem == newItem
    }
}
