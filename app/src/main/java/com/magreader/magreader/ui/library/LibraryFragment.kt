package com.magreader.magreader.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.magreader.magreader.R
import com.magreader.magreader.data.OpdsEntry
import com.magreader.magreader.data.OpdsManager
import com.magreader.magreader.databinding.FragmentLibraryBinding
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private lateinit var opdsManager: OpdsManager
    private lateinit var adapter: LibraryAdapter
    
    private var currentEntries = mutableListOf<OpdsEntry>()
    private var nextUrl: String? = null
    private var isLoading = false
    private var isOfflineMode = false
    private var isGridView = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        opdsManager = OpdsManager(requireContext())
        isOfflineMode = arguments?.getBoolean("isOfflineMode") ?: false
        
        if (!isOfflineMode && opdsManager.opdsUrl == null) {
            findNavController().navigate(R.id.nav_login)
            return
        }

        adapter = LibraryAdapter(
            opdsManager,
            onItemClick = { entry ->
                if (isOfflineMode || (entry.acquisitionUrl != null && (entry.type?.contains("pdf") == true || entry.acquisitionUrl!!.endsWith(".pdf")))) {
                    val filePath = if (isOfflineMode) entry.acquisitionUrl?.removePrefix("file://") else null
                    
                    if (filePath != null) {
                        val bundle = Bundle().apply {
                            putString("filePath", filePath)
                        }
                        findNavController().navigate(R.id.nav_reader, bundle)
                    } else {
                        val bundle = Bundle().apply {
                            putString("entryId", entry.id)
                            putString("title", entry.title)
                            putString("summary", entry.summary)
                            putString("thumbnailUrl", entry.thumbnailUrl)
                            putString("acquisitionUrl", entry.acquisitionUrl)
                        }
                        findNavController().navigate(R.id.action_library_to_book_detail, bundle)
                    }
                } else if (entry.acquisitionUrl != null && entry.type?.contains("atom+xml") == true) {
                    val bundle = Bundle().apply {
                        putString("subFeedUrl", entry.acquisitionUrl)
                        putBoolean("isOfflineMode", false)
                    }
                    findNavController().navigate(R.id.nav_library, bundle)
                }
            },
            onItemLongClick = { entry ->
                if (isOfflineMode) {
                    toggleSelectionMode(true)
                    adapter.toggleSelection(entry.id)
                    updateSelectionCount()
                }
            }
        )

        binding.buttonToggleLayout.setOnClickListener {
            isGridView = !isGridView
            updateLayout()
        }

        binding.buttonCancelSelection.setOnClickListener {
            toggleSelectionMode(false)
        }

        binding.buttonDeleteSelected.setOnClickListener {
            deleteSelectedItems()
        }

        updateLayout()

        if (!isOfflineMode) {
            binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = binding.recyclerView.layoutManager
                    if (layoutManager is LinearLayoutManager) {
                        val visibleItemCount = layoutManager.childCount
                        val totalItemCount = layoutManager.itemCount
                        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                        if (!isLoading && nextUrl != null) {
                            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0) {
                                loadFeed(nextUrl!!)
                            }
                        }
                    }
                }
            })
        }

        if (currentEntries.isEmpty()) {
            refreshContent()
        } else {
            adapter.submitList(currentEntries.toList())
        }
    }

    private fun toggleSelectionMode(enabled: Boolean) {
        adapter.isSelectionMode = enabled
        binding.layoutSelectionToolbar.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.buttonToggleLayout.visibility = if (enabled) View.GONE else View.VISIBLE
        if (enabled) updateSelectionCount()
    }

    private fun updateSelectionCount() {
        binding.textSelectionCount.text = "${adapter.selectedItems.size} selected"
    }

    private fun deleteSelectedItems() {
        val itemsToDelete = adapter.selectedItems.toList()
        if (itemsToDelete.isEmpty()) return

        itemsToDelete.forEach { entryId ->
            opdsManager.deleteBook(entryId)
        }
        
        toggleSelectionMode(false)
        refreshContent()
        Toast.makeText(context, "Deleted ${itemsToDelete.size} items", Toast.LENGTH_SHORT).show()
    }

    private fun refreshContent() {
        if (isOfflineMode) {
            loadOfflineContent()
        } else {
            val subFeedUrl = arguments?.getString("subFeedUrl")
            loadFeed(subFeedUrl ?: opdsManager.opdsUrl!!)
        }
    }

    private fun updateLayout() {
        adapter.isGridView = isGridView
        if (isGridView) {
            binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
            binding.buttonToggleLayout.setImageResource(R.drawable.ic_list_view_24dp)
        } else {
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.buttonToggleLayout.setImageResource(R.drawable.ic_grid_view_24dp)
        }
        binding.recyclerView.adapter = adapter
    }

    private fun loadOfflineContent() {
        binding.progressBar.visibility = View.VISIBLE
        val books = opdsManager.getOfflineBooks()
        currentEntries.clear()
        currentEntries.addAll(books)
        adapter.submitList(currentEntries.toList())
        binding.progressBar.visibility = View.GONE
        if (books.isEmpty()) {
            Toast.makeText(context, "No offline content found", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadFeed(url: String) {
        isLoading = true
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val feed = opdsManager.getFeed(url)
                if (feed != null) {
                    currentEntries.addAll(feed.entries)
                    adapter.submitList(currentEntries.toList())
                    nextUrl = feed.nextUrl
                } else {
                    Toast.makeText(context, "Failed to parse feed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading feed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                isLoading = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
