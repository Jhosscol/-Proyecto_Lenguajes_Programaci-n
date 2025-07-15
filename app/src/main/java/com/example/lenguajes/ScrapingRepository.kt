package com.example.lenguajes

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ScrapingRepository {
    private val apiKey = "Api Key" // ⚠️ Reemplaza con tu API key real

    private val serpApiService: SerpApiService by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://serpapi.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SerpApiService::class.java)
    }

    suspend fun buscarProducto(nombreProducto: String): Result<List<ProductoScraping>> {
        return try {
            val response = serpApiService.buscarProductos(
                query = nombreProducto,
                apiKey = apiKey
            )

            val productos = response.shopping_results?.map { result ->
                ProductoScraping(
                    nombre = result.title,
                    precio = extraerPrecio(result.price),
                    tienda = result.source,
                    url = result.link,
                    imagenUrl = result.thumbnail,
                    disponible = result.price != null
                )
            } ?: emptyList()

            Result.success(productos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extraerPrecio(precioTexto: String?): Double {
        if (precioTexto == null) return 0.0

        // Extraer número del texto de precio
        val regex = """[\d,]+\.?\d*""".toRegex()
        val match = regex.find(precioTexto.replace(",", ""))
        return match?.value?.toDoubleOrNull() ?: 0.0
    }
}
