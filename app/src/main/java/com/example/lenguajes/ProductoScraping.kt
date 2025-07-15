package com.example.lenguajes

data class ProductoScraping(
    val nombre: String,
    val precio: Double,
    val tienda: String,
    val url: String,
    val disponible: Boolean,
    val stock: Int? = null,
    val descuento: Double? = null,
    val imagenUrl: String? = null,
    val calificacion: Double? = null,
    val numeroReviews: Int? = null
) {
    companion object {
        fun fromShoppingResult(result: ShoppingResult): ProductoScraping? {
            return try {
                val nombre = result.title.takeIf { it.isNotBlank() } ?: return null
                val precio = extraerPrecioSeguro(result.price)
                val tienda = result.source.takeIf { it.isNotBlank() } ?: "Desconocido"
                val url = result.link.takeIf { it.isNotBlank() } ?: return null

                ProductoScraping(
                    nombre = nombre,
                    precio = precio,
                    tienda = tienda,
                    url = url,
                    imagenUrl = result.thumbnail,
                    disponible = precio > 0.0
                )
            } catch (e: Exception) {
                null // Retorna null si hay cualquier error
            }
        }

        private fun extraerPrecioSeguro(precioTexto: String?): Double {
            if (precioTexto.isNullOrBlank()) return 0.0

            return try {
                // Remover caracteres no numéricos excepto punto y coma
                val numeroLimpio = precioTexto
                    .replace(Regex("[^0-9.,]"), "")
                    .replace(",", "")

                if (numeroLimpio.isBlank()) return 0.0

                numeroLimpio.toDoubleOrNull() ?: 0.0
            } catch (e: Exception) {
                0.0
            }
        }
    }
}