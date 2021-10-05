package kr.ac.korea.oku.emergency.util

import android.content.res.Resources.getSystem

val Int.dp: Int
    get() = (this / getSystem().displayMetrics.density).toInt()

val Int.px: Int
    get() = (this * getSystem().displayMetrics.density).toInt()

val Int.floatPx: Float
    get() = this * getSystem().displayMetrics.density