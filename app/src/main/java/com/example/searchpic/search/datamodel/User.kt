package com.example.searchpic.search.datamodel

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class User (

    @SerializedName("id") val id : String,
    @SerializedName("username") val username : String,
    @SerializedName("name") val name : String,
    @SerializedName("first_name") val first_name : String,
    @SerializedName("last_name") val last_name : String,
    @SerializedName("instagram_username") val instagram_username : String,
    @SerializedName("twitter_username") val twitter_username : String,
    @SerializedName("portfolio_url") val portfolio_url : String,
    @SerializedName("profile_image") val profile_image : ProfileImage,
    @SerializedName("links") val links : Links
): Serializable