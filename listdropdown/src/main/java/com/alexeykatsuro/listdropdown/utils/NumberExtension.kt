package com.alexeykatsuro.listdropdown.utils

import android.content.res.Resources

val Float.toDp: Float
    get() = (this / Resources.getSystem().displayMetrics.density)
val Float.toPx: Float
    get() = (this * Resources.getSystem().displayMetrics.density)

val Int.toDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.toPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()