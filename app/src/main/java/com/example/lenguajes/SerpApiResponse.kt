package com.example.lenguajes

data class SerpApiResponse(
    val shopping_results: List<ShoppingResult>? = null,
    val search_metadata: SearchMetadata? = null
)
