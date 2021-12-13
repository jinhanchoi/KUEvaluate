package kr.ac.korea.oku.emergency.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

object LiveDataExtension {
    fun <T> LiveData<T>.observeOnce(owner: LifecycleOwner, observer: (T) -> Unit) {
        observe(owner, object: Observer<T> {
            override fun onChanged(value: T) {
                if(value != null) {
                    removeObserver(this)
                }
                observer(value)
            }
        })
    }
}