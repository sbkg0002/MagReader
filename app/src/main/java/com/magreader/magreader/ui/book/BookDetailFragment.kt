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
import com.magreader.magreader.data.OpdsEntry
import com.magreader.magreader.data.OpdsManager
import com.magreader.magreader.databinding.FragmentBookDetailBinding
import kotlinx.coroutines.launch
import okhttp3.Credentials

class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var opdsManager: OpdsManager
    
    private var entryId: String? = null
    private var title: String? = null
    private var summary: String? = null
    private var thumbnailUrl: String? = null
    private var acquisitionUrl: String? = null

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
        opdsManager = OpdsManager(requireContext())
        
        entryId = arguments?.getString("entryId")
        title = arguments?.getString("title")
        summary = arguments?.getString("summary")
        thumbnailUrl = arguments?.getString("thumbnailUrl")
        acquisitionUrl = arguments?.getString("acquisitionUrl")

        if (acquisitionUrl == null && entryId == null) {
            findNavController().popBackStack()
            return
        }

        binding.textTitle.text = title
        binding.textSummary.text = summary
        
        if (thumbnailUrl != null) {
            binding.imageThumbnail.load(thumbnailUrl) {
                crossfade(true)
                val user = opdsManager.username
                val pass = opdsManager.password
                if (!user.isNullOrEmpty() && !pass.isNullOrEmpty()) {
                    addHeader("Authorization", Credentials.basic(user, pass))
                }
            }
        }

        updateButtonState()

        binding.buttonRead.setOnClickListener {
            val localFile = entryId?.let { opdsManager.getLocalFile(it) }
            if (localFile != null && localFile.exists()) {
                val bundle = Bundle().apply {
                    putString("filePath", localFile.absolutePath)
                }
                findNavController().navigate(R.id.action_book_detail_to_reader, bundle)
            } else {
                downloadAndRead()
            }
        }
    }

    private fun updateButtonState() {
        val localFile = entryId?.let { opdsManager.getLocalFile(it) }
        if (localFile != null && localFile.exists()) {
            binding.buttonRead.text = "Read"
        } else {
            binding.buttonRead.text = getString(R.string.download_and_read)
        }
    }

    private fun downloadAndRead() {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonRead.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val entry = OpdsEntry(
                    id = entryId ?: "temp",
                    title = title ?: "Unknown",
                    summary = summary,
                    thumbnailUrl = thumbnailUrl,
                    acquisitionUrl = acquisitionUrl,
                    type = "application/pdf"
                )
                val file = opdsManager.downloadBook(entry)
                if (file != null && file.exists()) {
                    updateButtonState()
                    val bundle = Bundle().apply {
                        putString("filePath", file.absolutePath)
                    }
                    findNavController().navigate(R.id.action_book_detail_to_reader, bundle)
                } else {
                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.buttonRead.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
