package kr.ac.korea.oku.emergency.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.setVerticalSpace(space: Int) {
    addItemDecoration(object : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val itemCount = parent.adapter?.itemCount ?: 0

            if (position < itemCount - 1) {
                outRect.bottom = space
            }
        }
    })
}

fun RecyclerView.setHorizontalSpace(space: Int) {
    addItemDecoration(object : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val itemCount = parent.adapter?.itemCount ?: 0

            if (position < itemCount - 1) {
                outRect.right = space
            }
        }
    })
}