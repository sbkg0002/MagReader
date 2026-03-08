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
import com.magreader.magreader.R
import com.magreader.magreader.data.OpdsManager
import com.magreader.magreader.databinding.FragmentLibraryBinding
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private lateinit var opdsManager: OpdsManager
    private lateinit var adapter: LibraryAdapter

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
        
        if (opdsManager.opdsUrl == null) {
            findNavController().navigate(R.id.nav_login)
            return
        }

        adapter = LibraryAdapter(opdsManager) { entry ->
            if (entry.acquisitionUrl != null && entry.type?.contains("pdf") == true) {
                val bundle = Bundle().apply {
                    putString("entryId", entry.id)
                    putString("title", entry.title)
                    putString("summary", entry.summary)
                    putString("thumbnailUrl", entry.thumbnailUrl)
                    putString("acquisitionUrl", entry.acquisitionUrl)
                }
                findNavController().navigate(R.id.action_library_to_book_detail, bundle)
            } else if (entry.acquisitionUrl != null && entry.type?.contains("atom+xml") == true) {
                loadFeed(entry.acquisitionUrl)
            }
        }

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.adapter = adapter

        loadFeed(opdsManager.opdsUrl!!)
    }

    private fun loadFeed(url: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val feed = opdsManager.getFeed(url)
                if (feed != null) {
                    adapter.submitList(feed.entries)
                } else {
                    Toast.makeText(context, "Failed to parse feed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading feed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
