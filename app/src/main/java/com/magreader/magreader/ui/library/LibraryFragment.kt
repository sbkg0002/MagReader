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
import com.magreader.magreader.data.KomgaManager
import com.magreader.magreader.databinding.FragmentLibraryBinding
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private lateinit var komgaManager: KomgaManager
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
        komgaManager = KomgaManager(requireContext())
        
        if (komgaManager.serverUrl == null) {
            findNavController().navigate(R.id.nav_login)
            return
        }

        adapter = LibraryAdapter(komgaManager) { book ->
            val bundle = Bundle().apply {
                putString("bookId", book.id)
            }
            findNavController().navigate(R.id.action_library_to_book_detail, bundle)
        }

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.adapter = adapter

        loadBooks()
    }

    private fun loadBooks() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val api = komgaManager.api
                if (api != null) {
                    val response = api.getBooks(unpaged = false, size = 100)
                    adapter.submitList(response.content)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading books: ${e.message}", Toast.LENGTH_LONG).show()
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
