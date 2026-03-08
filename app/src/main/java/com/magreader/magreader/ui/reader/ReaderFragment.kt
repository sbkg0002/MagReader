package com.magreader.magreader.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.magreader.magreader.R
import com.magreader.magreader.data.KomgaManager
import com.magreader.magreader.databinding.FragmentReaderBinding
import kotlinx.coroutines.launch

class ReaderFragment : Fragment() {

    private var _binding: FragmentReaderBinding? = null
    private val binding get() = _binding!!
    private lateinit var komgaManager: KomgaManager
    private var bookId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReaderBinding.inflate(inflater, container, false)
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

        loadBook()
    }

    private fun loadBook() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val api = komgaManager.api
                if (api != null && bookId != null) {
                    val book = api.getBook(bookId!!)
                    val adapter = ReaderAdapter(book.id, book.media.pagesCount, komgaManager)
                    binding.viewPager.adapter = adapter
                    
                    binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            binding.textPageIndicator.text = getString(R.string.page_indicator, position + 1, book.media.pagesCount)
                        }
                    })
                    
                    binding.textPageIndicator.text = getString(R.string.page_indicator, 1, book.media.pagesCount)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading pages: ${e.message}", Toast.LENGTH_LONG).show()
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
