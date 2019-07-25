package com.alexeykatsuro.listdropdown

import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.widget.PopupWindow
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.widget.PopupWindowCompat
import androidx.recyclerview.widget.RecyclerView
import com.alexeyKasturo.util.TextPopupAdapter
import com.alexeyKasturo.util.TextPopupViewHolder
import com.alexeykatsuro.listdropdown.utils.toPx

typealias onItemSelected<T> = (index: Int, item: T) -> Unit

class DropdownPopup<T : Any>(
    private val context: Context,
    private val anchor: View,
    private val items: List<T>,
    private val mapToString: T.() -> String = { toString() },
    private val onSelected: onItemSelected<T>
) {


    private val popupWindow: PopupWindow
    private val adapter: RecyclerView.Adapter<TextPopupViewHolder<T>>
    private val attrs: AttributeSet? = null
    @AttrRes
    private val defStyleAttr: Int = R.attr.listPopupWindowStyle
    @StyleRes
    private val defStyleRes: Int = 0

    private var height: Int = WindowManager.LayoutParams.WRAP_CONTENT
    private var width: Int = WindowManager.LayoutParams.WRAP_CONTENT
    private var yOffSetIn = 0.toPx
    private var yOffSetOut: Int = 24.toPx
        set(value) {
            field = value + yOffSetIn
        }


    init {
        popupWindow = PopupWindow(context, attrs, defStyleAttr, defStyleRes)
        popupWindow.inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED

        subscribeOnLayoutChanges()

        val onClick = { index: Int, item: T ->
            onSelected(index, item)
            popupWindow.dismiss()
        }
        adapter = TextPopupAdapter(items, mapToString, onClick)
        val recyclerView = createRecycleView(context)
        recyclerView.adapter = adapter


        popupWindow.contentView = recyclerView
        PopupWindowCompat.setOverlapAnchor(popupWindow, false)

        calculateWidthHeight()
        popupWindow.height = height
        popupWindow.width = width
        popupWindow.isFocusable = true

        popupWindow.isClippingEnabled = false

        popupWindow.showAsDropDown(anchor, 0, yOffSetIn, Gravity.START)
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


    private fun calculateWidthHeight() {
        width = anchor.width
        height = popupWindow.getMaxAvailableHeight(anchor) - yOffSetOut
    }

    private fun createRecycleView(context: Context): RecyclerView {
        val root: ViewGroup = anchor.rootView as ViewGroup
        return LayoutInflater.from(context)
            .inflate(R.layout.dropdown_list, root, false) as RecyclerView
    }
}
