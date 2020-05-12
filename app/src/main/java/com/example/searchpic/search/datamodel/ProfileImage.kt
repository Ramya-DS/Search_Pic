package com.example.searchpic.search.datamodel

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProfileImage(
    @SerializedName("small") val small: String,
    @SerializedName("medium") val medium: String,
    @SerializedName("large") val large: String
) : Parcelable
