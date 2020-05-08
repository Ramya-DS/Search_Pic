package com.example.searchpic.search.datamodel

import com.google.gson.annotations.SerializedName

data class ProfileImage (

    @SerializedName("small") val small : String,
    @SerializedName("medium") val medium : String,
    @SerializedName("large") val large : String
)
