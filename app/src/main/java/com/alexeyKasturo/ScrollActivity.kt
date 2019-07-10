package com.alexeyKasturo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.scroll_layout.*

class ScrollActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scroll_layout)

        text_input1.setTextListener()
        text_input2.setTextListener()
        text_input3.setTextListener()
        text_input4.setTextListener()
        text_input5.setTextListener()
        text_input6.setTextListener()
        text_input7.setTextListener()
        text_input9.setTextListener()
        text_input11.setTextListener()
        text_input12.setTextListener()
        text_input13.setTextListener()
    }

    fun TextInputLayout.setTextListener() {
        val list = MutableList(10) {
            "Item $it"
        }
        setEndIconOnClickListener {
            DropdownPopup(this@ScrollActivity, this, list) { index: Int, item: String ->
                Toast.makeText(this@ScrollActivity, "i: $index, $item", Toast.LENGTH_SHORT).show()

            }
        }
    }
}