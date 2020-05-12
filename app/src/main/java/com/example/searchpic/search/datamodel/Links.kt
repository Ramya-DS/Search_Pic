package com.example.searchpic.search.datamodel

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Links(
    @SerializedName("self") val self: String,
    @SerializedName("html") val html: String,
    @SerializedName("download") val download: String?
) : Parcelable