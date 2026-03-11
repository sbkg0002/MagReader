package com.magreader.magreader.ui.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import coil.load
import com.magreader.magreader.R
import com.magreader.magreader.data.OpdsEntry
import com.magreader.magreader.data.OpdsManager
import com.magreader.magreader.databinding.ItemBookBinding
import com.magreader.magreader.databinding.ItemBookGridBinding
import okhttp3.Credentials

class LibraryAdapter(
    private val opdsManager: OpdsManager,
    private val onItemClick: (OpdsEntry) -> Unit,
    private val onItemLongClick: (OpdsEntry) -> Unit
) : ListAdapter<OpdsEntry, LibraryAdapter.BookViewHolder>(BookDiffCallback()) {

    var isGridView: Boolean = false
    var isSelectionMode: Boolean = false
        set(value) {
            field = value
            if (!value) selectedItems.clear()
            notifyDataSetChanged()
        }
    val selectedItems = mutableSetOf<String>()

    override fun getItemViewType(position: Int): Int {
        return if (isGridView) VIEW_TYPE_GRID else VIEW_TYPE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = if (viewType == VIEW_TYPE_GRID) {
            ItemBookGridBinding.inflate(inflater, parent, false)
        } else {
            ItemBookBinding.inflate(inflater, parent, false)
        }
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun toggleSelection(entryId: String) {
        if (selectedItems.contains(entryId)) {
            selectedItems.remove(entryId)
        } else {
            selectedItems.add(entryId)
        }
        notifyDataSetChanged()
    }

    inner class BookViewHolder(private val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: OpdsEntry) {
            val titleView = when (binding) {
                is ItemBookBinding -> binding.textTitle
                is ItemBookGridBinding -> binding.textTitle
                else -> null
            }
            val imageView = when (binding) {
                is ItemBookBinding -> binding.imageThumbnail
                is ItemBookGridBinding -> binding.imageThumbnail
                else -> null
            }
            val checkBox = when (binding) {
                is ItemBookBinding -> binding.checkboxSelect
                is ItemBookGridBinding -> binding.checkboxSelect
                else -> null
            }

            titleView?.text = entry.title
            
            imageView?.load(entry.thumbnailUrl) {
                crossfade(true)
                placeholder(R.drawable.icon_watermark)
                error(R.drawable.icon_watermark)
                val user = opdsManager.username
                val pass = opdsManager.password
                if (!user.isNullOrEmpty() && !pass.isNullOrEmpty()) {
                    addHeader("Authorization", Credentials.basic(user, pass))
                }
            }

            if (isSelectionMode) {
                checkBox?.visibility = View.VISIBLE
                checkBox?.isChecked = selectedItems.contains(entry.id)
            } else {
                checkBox?.visibility = View.GONE
            }

            checkBox?.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedItems.add(entry.id)
                else selectedItems.remove(entry.id)
            }

            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(entry.id)
                } else {
                    onItemClick(entry)
                }
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(entry)
                true
            }
        }
    }

    class BookDiffCallback : DiffUtil.ItemCallback<OpdsEntry>() {
        override fun areItemsTheSame(oldItem: OpdsEntry, newItem: OpdsEntry): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: OpdsEntry, newItem: OpdsEntry): Boolean = oldItem == newItem
    }

    companion object {
        private const val VIEW_TYPE_LIST = 0
        private const val VIEW_TYPE_GRID = 1
    }
}
