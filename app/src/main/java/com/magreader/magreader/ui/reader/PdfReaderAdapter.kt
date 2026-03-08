package com.magreader.magreader.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.magreader.magreader.databinding.ItemPageBinding

class PdfReaderAdapter(private val renderer: PdfRenderer) : RecyclerView.Adapter<PdfReaderAdapter.PdfViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val binding = ItemPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PdfViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = renderer.pageCount

    inner class PdfViewHolder(private val binding: ItemPageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pageIndex: Int) {
            val page = renderer.openPage(pageIndex)
            
            // Create a bitmap with the size of the page
            // Adjust scale if necessary for performance/quality
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            binding.imagePage.setImageBitmap(bitmap)
            page.close()
        }
    }
}
