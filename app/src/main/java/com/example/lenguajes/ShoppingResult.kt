package com.example.lenguajes

data class ShoppingResult(
    val position: Int? = null,
    val title: String = "",
    val link: String = "",
    val price: String? = null,
    val extracted_price: Double? = null,
    val rating: Double? = null,
    val reviews: Int? = null,
    val source: String = "",
    val thumbnail: String? = null,
    val delivery: String? = null
)
data class SearchMetadata(
    val id: String,
    val status: String,
    val json_endpoint: String,
    val created_at: String,
    val processed_at: String,
    val total_time_taken: Double
)