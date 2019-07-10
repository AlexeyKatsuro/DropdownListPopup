package com.alexeyKasturo

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.lwo.trafficpolice.ui.widgets.CustomListPopup
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            val  intent = Intent(this, ScrollActivity::class.java)
            startActivity(intent)
        }

        val COUNTRIES =  MutableList(10){
            "Item $it"
        }
        val adapter = ArrayAdapter(
            this,
            R.layout.dropdown_item,
            COUNTRIES
        )
        val mListPopupWindow = CustomListPopup(this)

        mListPopupWindow.anchorView = text_input
        mListPopupWindow.setOverlapAnchor(false)
        mListPopupWindow.width = ViewGroup.LayoutParams.WRAP_CONTENT
        mListPopupWindow.height = ViewGroup.LayoutParams.WRAP_CONTENT
        mListPopupWindow.isModal = true
        mListPopupWindow.setAdapter(adapter)
        mListPopupWindow.setOnItemClickListener(AdapterView.OnItemClickListener { parent, view, position, id ->
            Toast.makeText(this@MainActivity, COUNTRIES[position], Toast.LENGTH_SHORT).show()
            mListPopupWindow.dismiss()
        })



        text_input0.setTextListener()
        text_input.setTextListener()
        text_input1.setTextListener()

        text_input.setEndIconOnClickListener {
            mListPopupWindow.show()
        }

    }

    fun TextInputLayout.setTextListener() {
        val list = MutableList(10){
            "Item $it"
        }
        setEndIconOnClickListener {
            DropdownPopup(this@MainActivity, this, list) { index: Int, item: String ->
                Toast.makeText(this@MainActivity, "i: $index, $item", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


fun RecyclerView.setDividerItemDecoration() {
    val lm = layoutManager as LinearLayoutManager
    val dividerItemDecoration = DividerItemDecoration(context, lm.orientation)
    addItemDecoration(dividerItemDecoration)
}

val Float.toDp: Float
    get() = (this / Resources.getSystem().displayMetrics.density)
val Float.toPx: Float
    get() = (this * Resources.getSystem().displayMetrics.density)