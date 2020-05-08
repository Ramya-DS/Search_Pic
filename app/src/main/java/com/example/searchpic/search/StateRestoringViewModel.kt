package com.example.searchpic.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.searchpic.search.datamodel.ImageDetails

class StateRestoringViewModel(application: Application) : AndroidViewModel(application) {

    var resultList = mutableListOf<ImageDetails>()
    var mQuery: String = ""
    var scrollPosition: Int = 0
}