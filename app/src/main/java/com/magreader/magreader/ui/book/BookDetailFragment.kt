package com.magreader.magreader.ui.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.magreader.magreader.R
import com.magreader.magreader.data.KomgaManager
import com.magreader.magreader.databinding.FragmentBookDetailBinding
import kotlinx.coroutines.launch
import okhttp3.Credentials

class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var komgaManager: KomgaManager
    private var bookId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        komgaManager = KomgaManager(requireContext())
        bookId = arguments?.getString("bookId")

        if (bookId == null) {
            findNavController().popBackStack()
            return
        }

        loadBookDetails()

        binding.buttonRead.setOnClickListener {
            val bundle = Bundle().apply {
                putString("bookId", bookId)
            }
            findNavController().navigate(R.id.action_book_detail_to_reader, bundle)
        }
    }

    private fun loadBookDetails() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val api = komgaManager.api
                if (api != null && bookId != null) {
                    val book = api.getBook(bookId!!)
                    binding.textTitle.text = book.metadata.title
                    binding.textSummary.text = book.metadata.summary

                    val url = "${komgaManager.serverUrl}/api/v1/books/${book.id}/thumbnail"
                    binding.imageThumbnail.load(url) {
                        crossfade(true)
                        addHeader("Authorization", Credentials.basic(komgaManager.username ?: "", komgaManager.password ?: ""))
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading book: ${e.message}", Toast.LENGTH_LONG).show()
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
