package com.example.lenguajes

import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding4.widget.textChanges
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.lenguajes.PrecioPredictor
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var scrapingViewModel: ScrapingViewModel
    private lateinit var precioPredictor: PrecioPredictor
    private val disposables = CompositeDisposable()
    private lateinit var searchEditText: EditText
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var btnCrearAlerta: Button

    // Referencias a las vistas
    private lateinit var etBusqueda: EditText
    private lateinit var btnBuscar: Button
    private lateinit var btnAnalizar: Button
    private lateinit var btnOportunidades: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvResultados: TextView
    private lateinit var tvAnalisis: TextView
    private lateinit var chkDisponible: CheckBox
    private lateinit var chkGuardarBD: CheckBox
    private lateinit var spinnerOrden: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar base de datos y predictor
        databaseHelper = DatabaseHelper(this)
        precioPredictor = PrecioPredictor(databaseHelper)

        // Inicializar vistas
        etBusqueda = findViewById(R.id.etBusqueda)
        btnBuscar = findViewById(R.id.btnBuscar)
        btnAnalizar = findViewById(R.id.btnAnalizar)
        btnOportunidades = findViewById(R.id.btnOportunidades)
        progressBar = findViewById(R.id.progressBar)
        tvResultados = findViewById(R.id.tvResultados)
        tvAnalisis = findViewById(R.id.tvAnalisis)
        chkDisponible = findViewById(R.id.chkDisponible)
        chkGuardarBD = findViewById(R.id.chkGuardarBD)
        spinnerOrden = findViewById(R.id.spinnerOrden)

        // Inicializar ViewModel
        scrapingViewModel = ViewModelProvider(this)[ScrapingViewModel::class.java]

        setupObservers()
        setupUI()
        initializeViews()

        configurarBusquedaInteligente()
        iniciarServicioActualizacion()
    }
    private fun initializeViews() {
        searchEditText = findViewById(R.id.etBusqueda)
        progressBar = findViewById(R.id.progressBar)
        tvResultados = findViewById(R.id.tvResultados)
        btnCrearAlerta = findViewById(R.id.btnCrearAlerta)
    }

    private fun configurarBusquedaInteligente() {
        val subscription = searchEditText.textChanges()
            .debounce(800, TimeUnit.MILLISECONDS)
            .filter { query -> query.length > 2 }
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                progressBar.visibility = View.VISIBLE
                tvResultados.text = "🔍 Buscando productos..."
                btnCrearAlerta.visibility = View.GONE
            }
            .observeOn(Schedulers.io())
            .switchMap { query ->
                databaseHelper.buscarProductosRx(query.toString())
                    .toObservable()
                    .doOnNext { productos ->
                        // Registrar búsqueda en métricas
                        databaseHelper.registrarMetrica(
                            "busqueda_realizada",
                            "query:$query,resultados:${productos.size}",
                            "usuario"
                        )
                    }
                    .onErrorReturn { error ->
                        // Registrar error de búsqueda
                        databaseHelper.registrarEvento(
                            "ERROR_BUSQUEDA",
                            0,
                            "{\"query\": \"$query\", \"error\": \"${error.message}\"}"
                        )
                        emptyList()
                    }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { productos ->
                progressBar.visibility = View.GONE
                mostrarResultadosBusqueda(productos)
            }

        disposables.add(subscription)
    }

    private fun mostrarResultadosBusqueda(productos: List<Producto>) {
        val resultado = StringBuilder()

        if (productos.isEmpty()) {
            resultado.append("❌ No se encontraron productos\n")
            resultado.append("Intenta con términos diferentes\n")
            btnCrearAlerta.visibility = View.GONE
        } else {
            resultado.append("🔍 RESULTADOS DE BÚSQUEDA\n")
            resultado.append("═══════════════════════════════\n\n")

            productos.forEachIndexed { index, producto ->
                resultado.append("📦 ${index + 1}. ${producto.nombre}\n")
                resultado.append("💰 Precio: $. ${String.format("%.2f", producto.precio)}\n")
                resultado.append("🏪 Tienda: ${producto.tienda}\n")
                resultado.append("📂 Categoría: ${producto.categoria}\n")
                resultado.append("🔗 URL: ${producto.url}\n")
                resultado.append("─────────────────────────────\n")
            }

            // Mostrar botón para crear alerta del producto más barato
            val productoMasBarato = productos.minByOrNull { it.precio }
            if (productoMasBarato != null) {
                configurarBotonCrearAlerta(productoMasBarato)
                btnCrearAlerta.visibility = View.VISIBLE
            }
        }

        tvResultados.text = resultado.toString()
    }

    private fun configurarBotonCrearAlerta(producto: Producto) {
        btnCrearAlerta.text = "🔔 Crear Alerta para ${producto.nombre}"
        btnCrearAlerta.setOnClickListener {
            mostrarDialogoCrearAlerta(producto)
        }
    }

    private fun mostrarDialogoCrearAlerta(producto: Producto) {
        val builder = AlertDialog.Builder(this)
        val input = EditText(this)

        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Precio objetivo (menor a $. ${String.format("%.2f", producto.precio)})"

        builder.setTitle("🎯 Crear Alerta de Precio")
        builder.setMessage("Te notificaremos cuando ${producto.nombre} alcance el precio objetivo")
        builder.setView(input)

        builder.setPositiveButton("✅ Crear Alerta") { _, _ ->
            val precioObjetivoText = input.text.toString()
            if (precioObjetivoText.isNotEmpty()) {
                try {
                    val precioObjetivo = precioObjetivoText.toDouble()
                    if (precioObjetivo > 0 && precioObjetivo < producto.precio) {
                        crearAlertaPrecio(producto.nombre, precioObjetivo)
                    } else {
                        Toast.makeText(this, "❌ El precio objetivo debe ser menor al precio actual", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "❌ Ingresa un precio válido", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.setNegativeButton("❌ Cancelar", null)
        builder.show()
    }

    private fun crearAlertaPrecio(nombreProducto: String, precioObjetivo: Double) {
        val alertaId = databaseHelper.crearAlerta(nombreProducto, precioObjetivo, "PRECIO_BAJO")

        if (alertaId > 0) {
            // Registrar evento de creación de alerta
            databaseHelper.registrarEvento(
                "ALERTA_CREADA",
                0,
                "{\"producto\": \"$nombreProducto\", \"precio_objetivo\": $precioObjetivo, \"alerta_id\": $alertaId}"
            )

            // Registrar métrica
            databaseHelper.registrarMetrica(
                "alerta_creada",
                "precio_objetivo:$precioObjetivo",
                "usuario"
            )

            Toast.makeText(
                this,
                "✅ Alerta creada - Te notificaremos cuando $nombreProducto baje a $. ${String.format("%.2f", precioObjetivo)}",
                Toast.LENGTH_LONG
            ).show()

            btnCrearAlerta.visibility = View.GONE
        } else {
            Toast.makeText(this, "❌ Error al crear la alerta", Toast.LENGTH_SHORT).show()
        }
    }

    private fun iniciarServicioActualizacion() {
        val serviceIntent = Intent(this, ActualizacionService::class.java)
        startService(serviceIntent)

        // Registrar inicio del servicio
        databaseHelper.registrarEvento(
            "SERVICIO_INICIADO",
            0,
            "{\"timestamp\": ${System.currentTimeMillis()}}"
        )
    }

    // Función para ver alertas activas
    private fun mostrarAlertasActivas() {
        val alertasActivas = databaseHelper.obtenerProductosConAlertasActivas()

        if (alertasActivas.isEmpty()) {
            Toast.makeText(this, "ℹ️ No tienes alertas activas", Toast.LENGTH_SHORT).show()
        } else {
            val resultado = StringBuilder()
            resultado.append("🔔 ALERTAS ACTIVAS\n")
            resultado.append("═══════════════════════════════\n\n")

            alertasActivas.forEach { alerta ->
                resultado.append("📦 ${alerta.nombre}\n")
                resultado.append("💰 Precio actual: $. ${String.format("%.2f", alerta.precioActual)}\n")
                resultado.append("🎯 Precio objetivo: $. ${String.format("%.2f", alerta.precioObjetivo)}\n")
                resultado.append("🏪 Tienda: ${alerta.tienda}\n")
                resultado.append("📊 Tipo: ${alerta.tipoAlerta}\n")
                resultado.append("─────────────────────────────\n")
            }

            tvResultados.text = resultado.toString()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private fun setupObservers() {
        scrapingViewModel.productos.observe(this) { productos ->
            mostrarProductos(productos)

            // Guardar en BD si está habilitado
            if (chkGuardarBD.isChecked) {
                guardarProductosEnBD(productos)
            }
        }

        scrapingViewModel.loading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnBuscar.isEnabled = !isLoading
            btnAnalizar.isEnabled = !isLoading
            btnOportunidades.isEnabled = !isLoading
            btnBuscar.text = if (isLoading) "🔄 Buscando..." else "🔍 Buscar Productos"
        }

        scrapingViewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, "❌ Error: $it", Toast.LENGTH_LONG).show()
                tvResultados.text = "❌ Error al buscar productos. Verifica tu conexión a internet y tu API key."
            }
        }
    }

    private fun setupUI() {
        btnBuscar.setOnClickListener {
            val query = etBusqueda.text.toString().trim()
            if (query.isNotEmpty()) {
                scrapingViewModel.buscarProductos(query)
            } else {
                Toast.makeText(this, "⚠️ Ingresa un producto a buscar", Toast.LENGTH_SHORT).show()
            }
        }

        btnAnalizar.setOnClickListener {
            val query = etBusqueda.text.toString().trim()
            if (query.isNotEmpty()) {
                analizarProducto(query)
            } else {
                Toast.makeText(this, "⚠️ Ingresa un producto para analizar", Toast.LENGTH_SHORT).show()
            }
        }

        btnOportunidades.setOnClickListener {
            mostrarOportunidades()
        }
    }

    private fun guardarProductosEnBD(productos: List<ProductoScraping>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                productos.forEach { producto ->
                    databaseHelper.insertarOActualizarProducto(producto)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "✅ Productos guardados en BD", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "❌ Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun analizarProducto(nombreProducto: String) {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prediccion = precioPredictor.predecirPrecio(nombreProducto, 7)
                val volatilidad = precioPredictor.analizarVolatilidad(nombreProducto)
                val patron = precioPredictor.detectarPatronesEstacionales(nombreProducto)
                val recomendacion = precioPredictor.analizarMejorMomento(nombreProducto)
                val estadisticas = databaseHelper.obtenerEstadisticasPrecios(nombreProducto)
                val tendencia = databaseHelper.obtenerTendenciaPrecios(nombreProducto)

                withContext(Dispatchers.Main) {
                    mostrarAnalisis(prediccion, volatilidad, patron, recomendacion, estadisticas, tendencia)
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "❌ Error en análisis: ${e.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun mostrarAnalisis(
        prediccion: PrediccionPrecio?,
        volatilidad: AnalisisVolatilidad?,
        patron: PatronEstacional?,
        recomendacion: RecomendacionCompra?,
        estadisticas: EstadisticasPrecio?,
        tendencia: List<PuntoTendencia>
    ) {
        val analisis = StringBuilder()
        analisis.append("📊 ANÁLISIS PREDICTIVO\n")
        analisis.append("═══════════════════════════════\n\n")

        // Estadísticas históricas
        estadisticas?.let {
            analisis.append("📈 ESTADÍSTICAS HISTÓRICAS\n")
            analisis.append("─────────────────────────────\n")
            analisis.append("💰 Precio mínimo: $. ${String.format("%.2f", it.precioMinimo)}\n")
            analisis.append("💰 Precio máximo: $. ${String.format("%.2f", it.precioMaximo)}\n")
            analisis.append("💰 Precio promedio: $. ${String.format("%.2f", it.precioPromedio)}\n")
            analisis.append("📊 Registros analizados: ${it.totalRegistros}\n\n")
        }

        // Predicción de precios
        prediccion?.let {
            analisis.append("🔮 PREDICCIÓN (7 días)\n")
            analisis.append("─────────────────────────────\n")
            analisis.append("💲 Precio predicho: $. ${String.format("%.2f", it.precioPredicho)}\n")
            analisis.append("📊 Confianza: ${String.format("%.1f", it.confianza)}%\n")
            analisis.append("📈 Tendencia: ${it.tendencia}\n")
            analisis.append("🔄 Cambio esperado: $. ${String.format("%.2f", it.cambioEsperado)}\n\n")
        }

        // Análisis de volatilidad
        volatilidad?.let {
            analisis.append("📊 ANÁLISIS DE VOLATILIDAD\n")
            analisis.append("─────────────────────────────\n")
            analisis.append("📈 Volatilidad: ${it.volatilidad}\n")
            analisis.append("📊 Coeficiente de variación: ${String.format("%.2f", it.coeficienteVariacion)}%\n")
            analisis.append("📏 Rango de precios: $. ${String.format("%.2f", it.rangoPrecios)}\n\n")
        }

        // Patrones estacionales
        patron?.let {
            analisis.append("📅 PATRONES ESTACIONALES\n")
            analisis.append("─────────────────────────────\n")
            analisis.append("💚 Día más barato: ${it.diaMasBarato}\n")
            analisis.append("💸 Día más caro: ${it.diaMasCaro}\n")
            analisis.append("💰 Diferencia: $. ${String.format("%.2f", it.diferenciaPrecio)}\n")
            analisis.append("💡 ${it.recomendacion}\n\n")
        }

        // Recomendación final
        recomendacion?.let {
            analisis.append("🎯 RECOMENDACIÓN FINAL\n")
            analisis.append("─────────────────────────────\n")
            analisis.append("🚀 Acción: ${it.accion}\n")
            analisis.append("💬 ${it.mensaje}\n")
            analisis.append("📊 Confianza: ${String.format("%.1f", it.confianza)}%\n")
            analisis.append("📝 Factores considerados:\n")
            it.factores.forEach { factor ->
                analisis.append("   • $factor\n")
            }
            analisis.append("\n")
        }

        // Tendencia reciente
        if (tendencia.isNotEmpty()) {
            analisis.append("📈 TENDENCIA RECIENTE\n")
            analisis.append("─────────────────────────────\n")
            tendencia.take(5).forEach { punto ->
                analisis.append("📅 ${punto.fecha}: $. ${String.format("%.2f", punto.precio)}\n")
            }
        }

        if (analisis.length <= 100) {
            analisis.append("ℹ️ Datos insuficientes para análisis completo.\n")
            analisis.append("Realiza más búsquedas y guarda los datos en BD.\n")
        }

        tvAnalisis.text = analisis.toString()
    }

    private fun mostrarOportunidades() {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val oportunidades = databaseHelper.obtenerMejoresOportunidades()

                withContext(Dispatchers.Main) {
                    mostrarListaOportunidades(oportunidades)
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun mostrarListaOportunidades(oportunidades: List<OportunidadCompra>) {
        val resultado = StringBuilder()
        resultado.append("🎯 MEJORES OPORTUNIDADES\n")
        resultado.append("═══════════════════════════════\n\n")

        if (oportunidades.isEmpty()) {
            resultado.append("ℹ️ No hay oportunidades disponibles.\n")
            resultado.append("Realiza más búsquedas y guarda los datos.\n")
        } else {
            oportunidades.forEachIndexed { index, oportunidad ->
                resultado.append("🏆 #${index + 1} ${oportunidad.nombre}\n")
                resultado.append("💰 Precio actual: $. ${String.format("%.2f", oportunidad.precioActual)}\n")
                resultado.append("🏪 Tienda: ${oportunidad.tienda}\n")
                resultado.append("📊 Precio promedio: $. ${String.format("%.2f", oportunidad.precioPromedio)}\n")
                resultado.append("💾 Precio mínimo: $. ${String.format("%.2f", oportunidad.precioMinimo)}\n")
                resultado.append("🔥 Descuento: ${String.format("%.1f", oportunidad.porcentajeDescuento)}%\n")
                resultado.append("─────────────────────────────\n")
            }
        }

        tvResultados.text = resultado.toString()
    }

    private fun filtrarProductosNoDeseados(productos: List<ProductoScraping>): List<ProductoScraping> {
        val palabrasClaveExcluidas = listOf(
            // Accesorios y fundas
            "case", "carcasa", "protector", "cable", "charger", "wireless", "funda", "cover", "shell",
            "bumper", "armor", "defender", "otterbox", "spigen", "clear case", "leather case",

            // Audio y video
            "earbuds", "headphones", "speakers", "airpods", "beats", "audio", "video", "cage",
            "microphone", "mic", "sound", "music", "headset", "earphones",

            // Accesorios de cámara y video
            "tripod", "gimbal", "stabilizer", "lens", "filter", "mount", "grip", "handle",
            "rig", "cage kit", "video cage", "camera", "photo", "dual handles",

            // Cargadores y cables
            "charging", "power", "battery", "cord", "usb", "lightning", "magsafe", "qi",
            "adapter", "wall charger", "car charger", "portable charger", "power bank",

            // Servicios y planes
            "plan", "installments", "service", "warranty", "insurance", "protection",
            "subscription", "monthly", "contract", "carrier", "activation",

            // Accesorios varios
            "accesorio", "accessory", "bundle", "kit", "stand", "dock", "holder",
            "screen protector", "tempered glass", "film", "skin", "decal", "sticker",

            // Partes y reparaciones
            "replacement", "repair", "parts", "screen", "display", "battery replacement",
            "back glass", "camera lens", "button", "speaker", "charging port",

            // Marcas de accesorios
            "belkin", "anker", "mophie", "logitech", "zagg", "tech21", "pelican",
            "lifeproof", "catalyst", "nomad", "peak design", "moment", "joby",

            // Palabras específicas que vi en tu resultado
            "khronos", "ultimate kit", "mobile video", "aspen", "smallrig", "b&h"
        )

        return productos.filter { producto ->
            val nombreLimpio = producto.nombre.lowercase()
            val tiendaLimpia = producto.tienda.lowercase()

            // Filtrar por palabras clave en el nombre
            val tieneAccesorio = palabrasClaveExcluidas.any { palabra ->
                nombreLimpio.contains(palabra)
            }

            // Filtrar tiendas especializadas en accesorios
            val esTiendaAccesorios = tiendaLimpia.contains("photo") ||
                    tiendaLimpia.contains("video") ||
                    tiendaLimpia.contains("audio") ||
                    tiendaLimpia.contains("accessory")

            // Filtrar productos con precios muy bajos (probable accesorio)
            val esMuyBarato = producto.precio < 100.0

            // Filtrar productos restaurados con precios sospechosos
            val esRestauradoSospechoso = nombreLimpio.contains("restored") &&
                    producto.precio < 500.0

            // Solo mantener si NO es accesorio, NO es de tienda de accesorios,
            // NO es muy barato (a menos que sea un iPhone legítimo)
            !tieneAccesorio && !esTiendaAccesorios && !(esMuyBarato && !esIphoneLegitimo(nombreLimpio)) && !esRestauradoSospechoso
        }
    }

    private fun esIphoneLegitimo(nombre: String): Boolean {
        // Verificar que sea realmente un iPhone y no un accesorio
        val esIphone = nombre.contains("iphone") || nombre.contains("apple")
        val tieneModelo = nombre.contains("15") || nombre.contains("14") ||
                nombre.contains("13") || nombre.contains("12") ||
                nombre.contains("11") || nombre.contains("pro") ||
                nombre.contains("max") || nombre.contains("mini")
        val tieneCapacidad = nombre.contains("128gb") || nombre.contains("256gb") ||
                nombre.contains("512gb") || nombre.contains("1tb") ||
                nombre.contains("64gb")

        return esIphone && tieneModelo && (tieneCapacidad || nombre.contains("unlocked"))
    }

    private fun aplicarFiltros(productos: List<ProductoScraping>): List<ProductoScraping> {
        var productosFiltrados = filtrarProductosNoDeseados(productos)

        // Aplicar filtro de disponibilidad si está marcado
        if (chkDisponible.isChecked) {
            productosFiltrados = productosFiltrados.filter { it.disponible }
        }

        // Filtro adicional por rango de precios (solo productos sobre $200 para evitar accesorios)
        productosFiltrados = productosFiltrados.filter { it.precio >= 200.0 }

        // Filtro para eliminar duplicados por nombre similar
        productosFiltrados = eliminarDuplicados(productosFiltrados)

        // Aplicar ordenamiento según el spinner
        val ordenSeleccionado = spinnerOrden.selectedItemPosition
        productosFiltrados = when (ordenSeleccionado) {
            0 -> productosFiltrados.sortedBy { it.precio } // Precio menor a mayor
            1 -> productosFiltrados.sortedByDescending { it.precio } // Precio mayor a menor
            2 -> productosFiltrados.sortedBy { it.nombre } // Nombre A-Z
            3 -> productosFiltrados.sortedByDescending { it.nombre } // Nombre Z-A
            else -> productosFiltrados
        }

        return productosFiltrados
    }

    private fun eliminarDuplicados(productos: List<ProductoScraping>): List<ProductoScraping> {
        val productosUnicos = mutableListOf<ProductoScraping>()
        val nombresVistos = mutableSetOf<String>()

        for (producto in productos) {
            val nombreNormalizado = producto.nombre.lowercase()
                .replace("\\s+".toRegex(), " ")
                .replace("[^a-z0-9\\s]".toRegex(), "")
                .trim()

            if (!nombresVistos.contains(nombreNormalizado)) {
                nombresVistos.add(nombreNormalizado)
                productosUnicos.add(producto)
            }
        }

        return productosUnicos
    }

    private fun mostrarProductos(productos: List<ProductoScraping>) {
        val productosFiltrados = aplicarFiltros(productos)

        if (productosFiltrados.isEmpty()) {
            tvResultados.text = "❌ No se encontraron productos."
            return
        }

        val resultado = StringBuilder()
        resultado.append("📊 RESULTADOS FILTRADOS\n")
        resultado.append("═══════════════════════════════\n")
        resultado.append("Productos mostrados: ${productosFiltrados.size}\n\n")

        productosFiltrados.forEach { producto ->
            resultado.append("📱 ${producto.nombre}\n")
            resultado.append("💰 $. ${String.format("%.2f", producto.precio)}\n")
            resultado.append("🏪 ${producto.tienda}\n")
            resultado.append("${if (producto.disponible) "✅ Disponible" else "❌ No disponible"}\n")
            resultado.append("─────────────────────────────\n")
        }

        // Bloque de estadísticas
        val mejorPrecio = productosFiltrados.minByOrNull { it.precio }
        val promedio = productosFiltrados.map { it.precio }.average()
        val precioMax = productosFiltrados.maxOfOrNull { it.precio } ?: 0.0
        val ahorro = if (mejorPrecio != null) precioMax - mejorPrecio.precio else 0.0

        resultado.append("\n📊 ESTADÍSTICAS\n")
        resultado.append("═══════════════════════════════\n")
        resultado.append("🥇 Mejor precio: $. ${String.format("%.2f", mejorPrecio?.precio ?: 0.0)}\n")
        resultado.append("🏪 Mejor tienda: ${mejorPrecio?.tienda ?: "N/A"}\n")
        resultado.append("📈 Precio promedio: $. ${String.format("%.2f", promedio)}\n")
        resultado.append("💡 Ahorro máximo: $. ${String.format("%.2f", ahorro)}\n")

        // Mostrar en el TextView
        tvResultados.text = resultado.toString()
    }

}

