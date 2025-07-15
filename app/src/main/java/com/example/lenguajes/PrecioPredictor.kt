package com.example.lenguajes

import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.*

open class PrecioPredictor(private val dbHelper: DatabaseHelper) {

    // Predecir precio futuro usando regresión lineal simple
    fun predecirPrecio(nombreProducto: String, diasFuturos: Int = 7): PrediccionPrecio? {
        val tendencia = dbHelper.obtenerTendenciaPrecios(nombreProducto)

        if (tendencia.size < 3) {
            return null // Necesitamos al menos 3 puntos para hacer predicción
        }

        // Convertir fechas a números para regresión lineal
        val puntos = tendencia.mapIndexed { index, punto ->
            Pair(index.toDouble(), punto.precio)
        }

        // Calcular regresión lineal
        val n = puntos.size
        val sumX = puntos.sumOf { it.first }
        val sumY = puntos.sumOf { it.second }
        val sumXY = puntos.sumOf { it.first * it.second }
        val sumX2 = puntos.sumOf { it.first * it.first }

        val pendiente = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        val intercepto = (sumY - pendiente * sumX) / n

        // Predecir precio futuro
        val xFuturo = n.toDouble() + diasFuturos
        val precioPredicho = pendiente * xFuturo + intercepto

        // Calcular confianza basada en R²
        val yPromedio = sumY / n
        val ssr = puntos.sumOf { (pendiente * it.first + intercepto - it.second).pow(2) }
        val sst = puntos.sumOf { (it.second - yPromedio).pow(2) }
        val r2 = 1 - (ssr / sst)
        val confianza = (r2 * 100).coerceIn(0.0, 100.0)

        // Determinar tendencia
        val tendenciaTexto = when {
            pendiente > 0.1 -> "SUBIDA"
            pendiente < -0.1 -> "BAJADA"
            else -> "ESTABLE"
        }

        return PrediccionPrecio(
            precioPredicho = precioPredicho,
            confianza = confianza,
            tendencia = tendenciaTexto,
            cambioEsperado = pendiente * diasFuturos,
            diasAnalizados = diasFuturos
        )
    }

    // Analizar volatilidad del precio
    fun analizarVolatilidad(nombreProducto: String): AnalisisVolatilidad? {
        val historial = dbHelper.obtenerHistorialPrecios(nombreProducto, "")

        if (historial.size < 5) {
            return null
        }

        val precios = historial.map { it.precio }
        val promedio = precios.average()
        val varianza = precios.map { (it - promedio).pow(2) }.average()
        val desviacionEstandar = sqrt(varianza)

        val coeficienteVariacion = (desviacionEstandar / promedio) * 100

        val volatilidad = when {
            coeficienteVariacion < 5 -> "BAJA"
            coeficienteVariacion < 15 -> "MEDIA"
            else -> "ALTA"
        }

        return AnalisisVolatilidad(
            volatilidad = volatilidad,
            coeficienteVariacion = coeficienteVariacion,
            desviacionEstandar = desviacionEstandar,
            rangoPrecios = precios.maxOrNull()!! - precios.minOrNull()!!
        )
    }

    // Detectar patrones estacionales
    fun detectarPatronesEstacionales(nombreProducto: String): PatronEstacional? {
        val tendencia = dbHelper.obtenerTendenciaPrecios(nombreProducto)

        if (tendencia.size < 14) {
            return null
        }

        // Agrupar por día de la semana
        val preciosPorDia = mutableMapOf<Int, MutableList<Double>>()

        tendencia.forEach { punto ->
            // Simulamos día de la semana basado en índice
            val diaSemana = (punto.fecha.hashCode() % 7).absoluteValue
            preciosPorDia.getOrPut(diaSemana) { mutableListOf() }.add(punto.precio)
        }

        // Encontrar el día más barato y más caro
        val promediosPorDia = preciosPorDia.mapValues { it.value.average() }
        val diaMasBarato = promediosPorDia.minByOrNull { it.value }?.key ?: 0
        val diaMasCaro = promediosPorDia.maxByOrNull { it.value }?.key ?: 0

        val diasSemana = arrayOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

        return PatronEstacional(
            diaMasBarato = diasSemana[diaMasBarato],
            diaMasCaro = diasSemana[diaMasCaro],
            diferenciaPrecio = promediosPorDia.values.maxOrNull()!! - promediosPorDia.values.minOrNull()!!,
            recomendacion = "Mejor momento para comprar: ${diasSemana[diaMasBarato]}"
        )
    }

    // Generar alerta de precio
    fun generarAlertaPrecio(nombreProducto: String, tienda: String): AlertaPrecio? {
        val estadisticas = dbHelper.obtenerEstadisticasPrecios(nombreProducto)
        val historial = dbHelper.obtenerHistorialPrecios(nombreProducto, tienda)

        if (estadisticas == null || historial.isEmpty()) {
            return null
        }

        val precioActual = historial.first().precio
        val umbralDescuento = estadisticas.precioPromedio * 0.85 // 15% descuento
        val umbralOferta = estadisticas.precioMinimo * 1.05 // 5% sobre precio mínimo

        val tipoAlerta = when {
            precioActual <= umbralOferta -> "EXCELENTE_PRECIO"
            precioActual <= umbralDescuento -> "BUEN_PRECIO"
            precioActual >= estadisticas.precioMaximo * 0.95 -> "PRECIO_ALTO"
            else -> "PRECIO_NORMAL"
        }

        val mensaje = when (tipoAlerta) {
            "EXCELENTE_PRECIO" -> "¡Precio excelente! Muy cerca del mínimo histórico"
            "BUEN_PRECIO" -> "Buen precio, por debajo del promedio"
            "PRECIO_ALTO" -> "Precio alto, considera esperar"
            else -> "Precio dentro del rango normal"
        }

        return AlertaPrecio(
            tipo = tipoAlerta,
            mensaje = mensaje,
            precioActual = precioActual,
            precioPromedio = estadisticas.precioPromedio,
            precioMinimo = estadisticas.precioMinimo,
            ahorroPotencial = maxOf(0.0, estadisticas.precioPromedio - precioActual)
        )
    }

    // Comparar precios entre tiendas
    fun compararTiendas(nombreProducto: String): List<ComparacionTienda> {
        val comparaciones = mutableListOf<ComparacionTienda>()

        // Aquí normalmente consultarías la base de datos
        // Por simplicidad, uso un enfoque directo

        return comparaciones
    }

    // Análisis de mejor momento para comprar
    fun analizarMejorMomento(nombreProducto: String): RecomendacionCompra? {
        val prediccion = predecirPrecio(nombreProducto)
        val volatilidad = analizarVolatilidad(nombreProducto)
        val patron = detectarPatronesEstacionales(nombreProducto)
        val alerta = generarAlertaPrecio(nombreProducto, "")

        if (prediccion == null || volatilidad == null || alerta == null) {
            return null
        }

        val recomendacion = when {
            alerta.tipo == "EXCELENTE_PRECIO" -> "COMPRAR_AHORA"
            alerta.tipo == "BUEN_PRECIO" && prediccion.tendencia == "SUBIDA" -> "COMPRAR_PRONTO"
            prediccion.tendencia == "BAJADA" && volatilidad.volatilidad != "ALTA" -> "ESPERAR"
            else -> "MONITOREAR"
        }

        val mensaje = when (recomendacion) {
            "COMPRAR_AHORA" -> "¡Compra ahora! El precio está en su mejor momento"
            "COMPRAR_PRONTO" -> "Considera comprar pronto, los precios podrían subir"
            "ESPERAR" -> "Espera un poco, los precios podrían bajar"
            else -> "Mantén vigilado el precio"
        }

        return RecomendacionCompra(
            accion = recomendacion,
            mensaje = mensaje,
            confianza = prediccion.confianza,
            factores = listOf(
                "Tendencia: ${prediccion.tendencia}",
                "Volatilidad: ${volatilidad.volatilidad}",
                "Alerta: ${alerta.tipo}",
                patron?.recomendacion ?: "Sin patrón estacional detectado"
            )
        )
    }
}

// Clases de datos para predicciones
data class PrediccionPrecio(
    val precioPredicho: Double,
    val confianza: Double,
    val tendencia: String,
    val cambioEsperado: Double,
    val diasAnalizados: Int
)

data class AnalisisVolatilidad(
    val volatilidad: String,
    val coeficienteVariacion: Double,
    val desviacionEstandar: Double,
    val rangoPrecios: Double
)

data class PatronEstacional(
    val diaMasBarato: String,
    val diaMasCaro: String,
    val diferenciaPrecio: Double,
    val recomendacion: String
)

data class AlertaPrecio(
    val tipo: String,
    val mensaje: String,
    val precioActual: Double,
    val precioPromedio: Double,
    val precioMinimo: Double,
    val ahorroPotencial: Double
)

data class ComparacionTienda(
    val tienda: String,
    val precio: Double,
    val disponible: Boolean,
    val ranking: Int
)

data class RecomendacionCompra(
    val accion: String,
    val mensaje: String,
    val confianza: Double,
    val factores: List<String>
)
data class Hecho(
    val predicado: String,
    val sujeto: String,
    val valor: Any? = null
)

data class Regla(
    val id: String,
    val condiciones: List<Condicion>,
    val conclusion: Hecho,
    val prioridad: Int = 1
)

data class Condicion(
    val predicado: String,
    val sujeto: String,
    val operador: String, // "=", ">", "<", ">=", "<=", "contains"
    val valor: Any
)
class MotorReglas {
    private val reglas = mutableListOf<Regla>()
    private val baseHechos = mutableListOf<Hecho>()

    fun agregarRegla(regla: Regla) {
        reglas.add(regla)
    }

    fun agregarHecho(hecho: Hecho) {
        baseHechos.add(hecho)
    }

    fun limpiarHechos() {
        baseHechos.clear()
    }

    // Inferir nuevos hechos basado en reglas
    fun inferir(): List<Hecho> {
        val hechosInferidos = mutableListOf<Hecho>()
        var cambiosEnIteracion = true

        // Ejecutar hasta que no se infieran nuevos hechos
        while (cambiosEnIteracion) {
            cambiosEnIteracion = false

            for (regla in reglas.sortedByDescending { it.prioridad }) {
                if (evaluarCondiciones(regla.condiciones)) {
                    val nuevoHecho = regla.conclusion

                    // Solo agregar si no existe ya
                    if (!existeHecho(nuevoHecho)) {
                        baseHechos.add(nuevoHecho)
                        hechosInferidos.add(nuevoHecho)
                        cambiosEnIteracion = true
                    }
                }
            }
        }

        return hechosInferidos
    }

    // Evaluar si todas las condiciones de una regla se cumplen
    private fun evaluarCondiciones(condiciones: List<Condicion>): Boolean {
        return condiciones.all { condicion ->
            evaluarCondicion(condicion)
        }
    }

    // Evaluar una condición individual
    private fun evaluarCondicion(condicion: Condicion): Boolean {
        val hechoRelacionado = baseHechos.find {
            it.predicado == condicion.predicado && it.sujeto == condicion.sujeto
        }

        return when (condicion.operador) {
            "=" -> hechoRelacionado?.valor == condicion.valor
            ">" -> (hechoRelacionado?.valor as? Number)?.toDouble()?.let {
                it > (condicion.valor as Number).toDouble()
            } ?: false
            "<" -> (hechoRelacionado?.valor as? Number)?.toDouble()?.let {
                it < (condicion.valor as Number).toDouble()
            } ?: false
            ">=" -> (hechoRelacionado?.valor as? Number)?.toDouble()?.let {
                it >= (condicion.valor as Number).toDouble()
            } ?: false
            "<=" -> (hechoRelacionado?.valor as? Number)?.toDouble()?.let {
                it <= (condicion.valor as Number).toDouble()
            } ?: false
            "contains" -> hechoRelacionado?.valor?.toString()?.contains(condicion.valor.toString()) ?: false
            else -> false
        }
    }

    private fun existeHecho(hecho: Hecho): Boolean {
        return baseHechos.any {
            it.predicado == hecho.predicado &&
                    it.sujeto == hecho.sujeto &&
                    it.valor == hecho.valor
        }
    }

    // Consultar hechos
    fun consultarHechos(predicado: String, sujeto: String): List<Hecho> {
        return baseHechos.filter { it.predicado == predicado && it.sujeto == sujeto }
    }

    fun todosLosHechos(): List<Hecho> {
        return baseHechos.toList()
    }
}
// Clase para resultado del análisis lógico
data class AnalisisLogico(
    val recomendacionFinal: String,
    val hechosInferidos: List<Hecho>,
    val todosLosHechos: List<Hecho>
)
class PrecioPredictorConLogica(private val dbHelper: DatabaseHelper) : PrecioPredictor(dbHelper) {

    private val motorReglas = MotorReglas()

    init {
        configurarReglasDeNegocio()
    }

    // Configurar reglas de negocio en formato lógico
    private fun configurarReglasDeNegocio() {
        // Regla 1: Si precio < 80% del promedio → es barato
        motorReglas.agregarRegla(Regla(
            id = "precio_barato",
            condiciones = listOf(
                Condicion("porcentaje_promedio", "producto", "<", 80.0)
            ),
            conclusion = Hecho("clasificacion_precio", "producto", "BARATO"),
            prioridad = 3
        ))

        // Regla 2: Si es barato Y tendencia no es subida → excelente oportunidad
        motorReglas.agregarRegla(Regla(
            id = "excelente_oportunidad",
            condiciones = listOf(
                Condicion("clasificacion_precio", "producto", "=", "BARATO"),
                Condicion("tendencia", "producto", "=", "BAJADA")
            ),
            conclusion = Hecho("oportunidad", "producto", "EXCELENTE"),
            prioridad = 5
        ))

        // Regla 3: Si volatilidad alta Y tendencia bajada → esperar
        motorReglas.agregarRegla(Regla(
            id = "esperar_volatilidad",
            condiciones = listOf(
                Condicion("volatilidad", "producto", "=", "ALTA"),
                Condicion("tendencia", "producto", "=", "BAJADA")
            ),
            conclusion = Hecho("recomendacion", "producto", "ESPERAR"),
            prioridad = 4
        ))

        // Regla 4: Si es viernes Y mes noviembre → aplicar factor black friday
        motorReglas.agregarRegla(Regla(
            id = "black_friday",
            condiciones = listOf(
                Condicion("dia_semana", "fecha", "=", "VIERNES"),
                Condicion("mes", "fecha", "=", "NOVIEMBRE")
            ),
            conclusion = Hecho("evento_especial", "fecha", "BLACK_FRIDAY"),
            prioridad = 2
        ))

        // Regla 5: Si evento black friday Y clasificacion barato → comprar inmediatamente
        motorReglas.agregarRegla(Regla(
            id = "comprar_black_friday",
            condiciones = listOf(
                Condicion("evento_especial", "fecha", "=", "BLACK_FRIDAY"),
                Condicion("clasificacion_precio", "producto", "=", "BARATO")
            ),
            conclusion = Hecho("recomendacion", "producto", "COMPRAR_INMEDIATAMENTE"),
            prioridad = 6
        ))
    }

    // Análisis inteligente usando programación lógica
    @RequiresApi(Build.VERSION_CODES.O)
    fun analizarConLogica(nombreProducto: String): AnalisisLogico {
        // Limpiar hechos previos
        motorReglas.limpiarHechos()

        // Obtener datos del análisis existente
        val prediccion = predecirPrecio(nombreProducto)
        val volatilidad = analizarVolatilidad(nombreProducto)
        val estadisticas = dbHelper.obtenerEstadisticasPrecios(nombreProducto)

        if (prediccion == null || volatilidad == null || estadisticas == null) {
            return AnalisisLogico("Error: Datos insuficientes", emptyList(), emptyList())
        }

        // Cargar hechos en el motor
        val historial = dbHelper.obtenerHistorialPrecios(nombreProducto, "")
        val precioActual = historial.firstOrNull()?.precio ?: 0.0
        val porcentajePromedio = (precioActual / estadisticas.precioPromedio) * 100

        // Agregar hechos al motor
        motorReglas.agregarHecho(Hecho("precio_actual", "producto", precioActual))
        motorReglas.agregarHecho(Hecho("precio_promedio", "producto", estadisticas.precioPromedio))
        motorReglas.agregarHecho(Hecho("porcentaje_promedio", "producto", porcentajePromedio))
        motorReglas.agregarHecho(Hecho("tendencia", "producto", prediccion.tendencia))
        motorReglas.agregarHecho(Hecho("volatilidad", "producto", volatilidad.volatilidad))

        // Agregar hechos de fecha (simulados)
        val fechaActual = java.time.LocalDate.now()
        motorReglas.agregarHecho(Hecho("dia_semana", "fecha", fechaActual.dayOfWeek.name))
        motorReglas.agregarHecho(Hecho("mes", "fecha", fechaActual.month.name))

        // Ejecutar inferencia
        val hechosInferidos = motorReglas.inferir()

        // Obtener recomendación final
        val recomendaciones = motorReglas.consultarHechos("recomendacion", "producto")
        val oportunidades = motorReglas.consultarHechos("oportunidad", "producto")

        val recomendacionFinal = when {
            recomendaciones.any { it.valor == "COMPRAR_INMEDIATAMENTE" } -> "COMPRAR_INMEDIATAMENTE"
            oportunidades.any { it.valor == "EXCELENTE" } -> "EXCELENTE_OPORTUNIDAD"
            recomendaciones.any { it.valor == "ESPERAR" } -> "ESPERAR"
            else -> "MONITOREAR"
        }

        return AnalisisLogico(
            recomendacionFinal = recomendacionFinal,
            hechosInferidos = hechosInferidos,
            todosLosHechos = motorReglas.todosLosHechos()
        )
    }

    // Agregar nuevas reglas dinámicamente
    fun agregarReglaDinamica(
        id: String,
        condiciones: List<Triple<String, String, Any>>, // predicado, operador, valor
        conclusion: Triple<String, String, Any>, // predicado, sujeto, valor
        prioridad: Int = 1
    ) {
        val condicionesRegla = condiciones.map { (predicado, operador, valor) ->
            Condicion(predicado, "producto", operador, valor)
        }

        val regla = Regla(
            id = id,
            condiciones = condicionesRegla,
            conclusion = Hecho(conclusion.first, conclusion.second, conclusion.third),
            prioridad = prioridad
        )

        motorReglas.agregarRegla(regla)
    }
}
