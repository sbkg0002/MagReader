package com.magreader.magreader.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.magreader.magreader.R
import com.magreader.magreader.data.OpdsManager
import com.magreader.magreader.databinding.FragmentReaderBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ReaderFragment : Fragment() {

    private var _binding: FragmentReaderBinding? = null
    private val binding get() = _binding!!
    private lateinit var opdsManager: OpdsManager
    private var pdfFile: File? = null
    private var pdfRenderer: PdfRenderer? = null

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
        opdsManager = OpdsManager(requireContext())
        val filePath = arguments?.getString("filePath")

        if (filePath == null) {
            findNavController().popBackStack()
            return
        }

        pdfFile = File(filePath)
        if (!pdfFile!!.exists()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        openRenderer()
    }

    private fun openRenderer() {
        try {
            val input = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(input)
            
            val pageCount = pdfRenderer!!.pageCount
            val adapter = PdfReaderAdapter(pdfRenderer!!)
            binding.viewPager.adapter = adapter
            
            binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    binding.textPageIndicator.text = getString(R.string.page_indicator, position + 1, pageCount)
                }
            })
            
            binding.textPageIndicator.text = getString(R.string.page_indicator, 1, pageCount)
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pdfRenderer?.close()
        _binding = null
    }
}
