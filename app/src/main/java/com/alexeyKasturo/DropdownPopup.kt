package com.alexeyKasturo

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.widget.PopupWindowCompat
import androidx.recyclerview.widget.RecyclerView

typealias onItemSelected<T> = (index: Int, item: T) -> Unit

class DropdownPopup<T : Any>(
    private val context: Context,
    private val anchor: View,
    private val items: List<T>,
    private val mapToString: T.() -> String = { toString() },
    private val onSelected: onItemSelected<T>
) {


    private val popupWindow: PopupWindow
    private val adapter: RecyclerView.Adapter<PopupViewHolder<T>>
    private val attrs: AttributeSet? = null
    @AttrRes
    private val defStyleAttr: Int = R.attr.listPopupWindowStyle
    @StyleRes
    private val defStyleRes: Int = 0

    init {
        popupWindow = PopupWindow(context, attrs, defStyleAttr, defStyleRes)
        popupWindow.inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED

        subscribeOnLayoutChanges()

        val onClick = { index: Int, item: T ->
            onSelected(index, item)
            popupWindow.dismiss()
        }
        adapter = PopupAdapter(items, mapToString, onClick)
        val recyclerView = createRecycleView(context)
        recyclerView.adapter = adapter


        popupWindow.contentView = recyclerView
        //popupWindow.elevation = 20f
        PopupWindowCompat.setOverlapAnchor(popupWindow, false)
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        popupWindow.height = popupWindow.getMaxAvailableHeight(anchor) - 16f.toPx.toInt()
        popupWindow.width = anchor.width
        popupWindow.isFocusable = true

        popupWindow.isClippingEnabled = false



        popupWindow.showAsDropDown(anchor, 0, 4f.toPx.toInt(), Gravity.START)
    }

    private fun subscribeOnLayoutChanges() {
        val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            popupWindow.update()
        }
        anchor.addOnLayoutChangeListener(listener)
        popupWindow.setOnDismissListener {
            anchor.removeOnLayoutChangeListener(listener)
        }
    }


    fun createRecycleView(context: Context): RecyclerView {
        val root: ViewGroup = anchor.rootView as ViewGroup
        return LayoutInflater.from(context)
            .inflate(R.layout.dropdown_list, root, false) as RecyclerView
    }

    class PopupAdapter<I : Any>(
        private val items: List<I>,
        private val mapToString: I.() -> String,
        private val onSelected: (index: Int, item: I) -> Unit
    ) : RecyclerView.Adapter<PopupViewHolder<I>>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopupViewHolder<I> {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.dropdown_item, parent, false)
            return PopupViewHolder<I>(view).apply { setOnSelectListener(onSelected) }
        }

        override fun onBindViewHolder(holder: PopupViewHolder<I>, position: Int) {
            val item = items[position]
            holder.bind(item, item.mapToString(), position)
        }

        override fun getItemCount(): Int = items.size
    }
}
