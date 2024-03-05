package com.rafiansyah.cameradegree.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rafiansyah.cameradegree.databinding.ItemDeskripsiBinding
import com.rafiansyah.cameradegree.retrofit.response.PrediksiResponseItem
import com.rafiansyah.cameradegree.util.convert64toIMG


class RvAdapter(
    private val listItem: ArrayList<PrediksiResponseItem>,
    private val jarak: Double
) : RecyclerView.Adapter<RvAdapter.ListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding = ItemDeskripsiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val (panjang, kaki, gambar) = listItem[position]
        val lenList = listItem.size
        with(holder){
            binding.imgItemPhoto.setImageBitmap(convert64toIMG(gambar))
            if (lenList > 1){
                val layoutParams = binding.cardView.layoutParams
                val resources = binding.root.context.resources
                val displayMetrics = resources.displayMetrics
                layoutParams.width = displayMetrics.widthPixels / 2
                binding.cardView.layoutParams = layoutParams
            }
            binding.tvItemKelas.text=kaki
            val number3digits:Double = Math.round(panjang/31 * 1000.0) / 1000.0
            val number2digits:Double = Math.round(number3digits * 100.0) / 100.0
            binding.tvItemPanjang.text="${number2digits}cm"
        }
    }

    override fun getItemCount(): Int = listItem.size

    inner class ListViewHolder(val binding: ItemDeskripsiBinding) : RecyclerView.ViewHolder(binding.root)
}