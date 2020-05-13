package com.example.searchpic.util

import android.os.Bundle
import android.view.View

interface OnImageClickedListener {
    fun onImageClicked(imageBundle: Bundle,view: View)
}