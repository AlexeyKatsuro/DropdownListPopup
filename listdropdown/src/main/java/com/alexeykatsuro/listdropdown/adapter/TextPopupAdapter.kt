package com.alexeyKasturo.util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexeykatsuro.listdropdown.R

class TextPopupAdapter<I : Any>(
    private val items: List<I>,
    private val mapToString: I.() -> String,
    private val onSelected: (index: Int, item: I) -> Unit
) : RecyclerView.Adapter<TextPopupViewHolder<I>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextPopupViewHolder<I> {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.dropdown_item, parent, false)
        return TextPopupViewHolder<I>(view).apply { setOnSelectListener(onSelected) }
    }

    override fun onBindViewHolder(holder: TextPopupViewHolder<I>, position: Int) {
        val item = items[position]
        holder.bind(item, item.mapToString(), position)
    }

    override fun getItemCount(): Int = items.size
}