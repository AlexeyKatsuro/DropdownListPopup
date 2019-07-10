package com.alexeyKasturo

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PopupViewHolder<T : Any>(view: View) : RecyclerView.ViewHolder(view) {

    private val textView = view.findViewById<TextView>(R.id.text_dropdown_item)

    var item: T? = null
    var index: Int? = null

    fun bind(item: T, text: String, index: Int) {
        this.item = item
        this.index = index
        textView.text = text
    }

    fun setOnSelectListener(onSelected: (index: Int, T) -> Unit) {
        itemView.setOnClickListener {
            onSelected(requireNotNull(index), requireNotNull(item))
        }
    }

}