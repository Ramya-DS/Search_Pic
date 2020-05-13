package com.example.searchpic.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.searchpic.search.datamodel.ImageDetails

class SearchActivityViewModel(application: Application) : AndroidViewModel(application) {
    var mQuery: String = ""
    var optionQueryMap = HashMap<String, String>()
}