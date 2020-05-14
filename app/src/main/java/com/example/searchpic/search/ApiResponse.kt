package com.example.searchpic.search

import com.example.searchpic.search.datamodel.SearchResult
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap
//TODO needs an self-explanatory name. 'ApiResponse' is not relevant
interface ApiResponse {
    @GET("photos")
    fun getImageResultsList(@QueryMap parameters: Map<String, String>): Call<SearchResult>
}