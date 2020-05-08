package com.example.searchpic.search

import com.example.searchpic.search.datamodel.SearchResult
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiResponse {
    @GET("photos")
    fun getImageResultsList(@Query("query") query: String, @Query("page") page: Int, @Query("per_page") perPage: Int = 50): Call<SearchResult>
}