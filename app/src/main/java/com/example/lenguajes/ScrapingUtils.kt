package com.example.lenguajes


// Extensiones útiles para trabajar con productos
fun List<ProductoScraping>.ordenarPorPrecio(): List<ProductoScraping> {
    return this.sortedBy { it.precio }
}

fun List<ProductoScraping>.filtrarPorTienda(tienda: String): List<ProductoScraping> {
    return this.filter { it.tienda.contains(tienda, ignoreCase = true) }
}

fun List<ProductoScraping>.obtenerTiendas(): List<String> {
    return this.map { it.tienda }.distinct()
}

// Utilidades para testing
class ScrapingTestUtils {
    companion object {
        fun crearProductoTest(
            nombre: String = "iPhone 15",
            precio: Double = 800.0,
            tienda: String = "Amazon"
        ): ProductoScraping {
            return ProductoScraping(
                nombre = nombre,
                precio = precio,
                tienda = tienda,
                url = "https://example.com/product",
                disponible = true,
                imagenUrl = "https://example.com/image.jpg"
            )
        }

        fun simularBusquedaProductos(): List<ProductoScraping> {
            return listOf(
                crearProductoTest("iPhone 15", 850.0, "Amazon"),
                crearProductoTest("iPhone 15", 820.0, "MercadoLibre"),
                crearProductoTest("iPhone 15", 800.0, "Falabella"),
                crearProductoTest("iPhone 15", 880.0, "Ripley")
            )
        }
    }
}