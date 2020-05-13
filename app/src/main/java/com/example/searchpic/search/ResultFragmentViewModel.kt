package com.example.searchpic.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.searchpic.search.datamodel.ImageDetails

class ResultFragmentViewModel(application: Application) : AndroidViewModel(application) {
    var resultList = mutableListOf<ImageDetails>()
}