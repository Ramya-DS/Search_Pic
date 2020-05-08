package com.example.searchpic.search.datamodel

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Links(
    @SerializedName("self") val self: String,
    @SerializedName("html") val html: String,
    @SerializedName("download") val download: String
) : Serializable