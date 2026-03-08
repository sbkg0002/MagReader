package com.magreader.magreader.ui.reader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.magreader.magreader.data.KomgaManager
import com.magreader.magreader.databinding.ItemPageBinding
import okhttp3.Credentials

class ReaderAdapter(
    private val bookId: String,
    private val pageCount: Int,
    private val komgaManager: KomgaManager
) : RecyclerView.Adapter<ReaderAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(position + 1)
    }

    override fun getItemCount(): Int = pageCount

    inner class PageViewHolder(private val binding: ItemPageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pageNumber: Int) {
            val url = "${komgaManager.serverUrl}/api/v1/books/$bookId/pages/$pageNumber"
            
            binding.imagePage.load(url) {
                crossfade(true)
                addHeader("Authorization", Credentials.basic(komgaManager.username ?: "", komgaManager.password ?: ""))
            }
        }
    }
}
