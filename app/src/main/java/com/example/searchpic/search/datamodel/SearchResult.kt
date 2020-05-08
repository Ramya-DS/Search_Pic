package com.example.searchpic.search.datamodel

import com.google.gson.annotations.SerializedName

data class SearchResult(@SerializedName("total_pages") val totalPages: Int, @SerializedName("results") val results: List<ImageDetails>)
