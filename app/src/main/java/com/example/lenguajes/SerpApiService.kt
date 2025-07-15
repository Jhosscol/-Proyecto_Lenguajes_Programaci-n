package com.example.lenguajes

import retrofit2.http.GET
import retrofit2.http.Query

interface SerpApiService {
    @GET("search")
    suspend fun buscarProductos(
        @Query("engine") engine: String = "google_shopping",
        @Query("q") query: String,
        @Query("api_key") apiKey: String,
        @Query("location") location: String = "Lima, Peru",
        @Query("hl") language: String = "es",
        @Query("gl") country: String = "us",
        @Query("num") numResults: Int = 20
    ): SerpApiResponse
}