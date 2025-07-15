package com.example.lenguajes

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "productos.db"
        private const val DATABASE_VERSION = 1

        // Tabla de productos
        private const val TABLE_PRODUCTOS = "productos"
        private const val COL_ID = "id"
        private const val COL_NOMBRE = "nombre"
        private const val COL_PRECIO = "precio"
        private const val COL_TIENDA = "tienda"
        private const val COL_DISPONIBLE = "disponible"
        private const val COL_FECHA_REGISTRO = "fecha_registro"
        private const val COL_CATEGORIA = "categoria"
        private const val COL_URL = "url"

        // Tabla de historial de precios
        private const val TABLE_HISTORIAL = "historial_precios"
        private const val COL_HIST_ID = "id"
        private const val COL_HIST_PRODUCTO_ID = "producto_id"
        private const val COL_HIST_PRECIO = "precio"
        private const val COL_HIST_FECHA = "fecha"
        private const val COL_HIST_TIENDA = "tienda"
        // Tabla de alertas de usuario
        private const val TABLE_ALERTAS = "alertas_usuario"
        private const val COL_ALERT_ID = "id"
        private const val COL_ALERT_PRODUCTO_NOMBRE = "producto_nombre"
        private const val COL_ALERT_PRECIO_OBJETIVO = "precio_objetivo"
        private const val COL_ALERT_ACTIVA = "activa"
        private const val COL_ALERT_FECHA_CREACION = "fecha_creacion"
        private const val COL_ALERT_TIPO = "tipo" // "PRECIO_BAJO", "DISPONIBILIDAD", "DESCUENTO"
        private const val COL_ALERT_USUARIO_ID = "usuario_id"

        // Tabla de eventos del sistema
        private const val TABLE_EVENTOS = "eventos_sistema"
        private const val COL_EVENT_ID = "id"
        private const val COL_EVENT_TIPO = "tipo"
        private const val COL_EVENT_PRODUCTO_ID = "producto_id"
        private const val COL_EVENT_DATOS = "datos" // JSON con información del evento
        private const val COL_EVENT_FECHA = "fecha"
        private const val COL_EVENT_PROCESADO = "procesado"

        // Tabla de reglas de negocio (para programación lógica)
        private const val TABLE_REGLAS = "reglas_negocio"
        private const val COL_REGLA_ID = "id"
        private const val COL_REGLA_NOMBRE = "nombre"
        private const val COL_REGLA_CONDICIONES = "condiciones" // JSON
        private const val COL_REGLA_ACCION = "accion" // JSON
        private const val COL_REGLA_ACTIVA = "activa"
        private const val COL_REGLA_PRIORIDAD = "prioridad"

        // Tabla de notificaciones
        private const val TABLE_NOTIFICACIONES = "notificaciones"
        private const val COL_NOTIF_ID = "id"
        private const val COL_NOTIF_TITULO = "titulo"
        private const val COL_NOTIF_MENSAJE = "mensaje"
        private const val COL_NOTIF_TIPO = "tipo"
        private const val COL_NOTIF_FECHA = "fecha"
        private const val COL_NOTIF_LEIDA = "leida"
        private const val COL_NOTIF_PRODUCTO_ID = "producto_id"

        // Tabla de métricas y analytics
        private const val TABLE_METRICAS = "metricas"
        private const val COL_METRIC_ID = "id"
        private const val COL_METRIC_NOMBRE = "nombre"
        private const val COL_METRIC_VALOR = "valor"
        private const val COL_METRIC_FECHA = "fecha"
        private const val COL_METRIC_CATEGORIA = "categoria"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Crear tabla de productos
        val createProductosTable = """
            CREATE TABLE $TABLE_PRODUCTOS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE TEXT NOT NULL,
                $COL_PRECIO REAL NOT NULL,
                $COL_TIENDA TEXT NOT NULL,
                $COL_DISPONIBLE INTEGER NOT NULL,
                $COL_FECHA_REGISTRO TEXT NOT NULL,
                $COL_CATEGORIA TEXT,
                $COL_URL TEXT
            )
        """.trimIndent()

        // Crear tabla de historial de precios
        val createHistorialTable = """
            CREATE TABLE $TABLE_HISTORIAL (
                $COL_HIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_HIST_PRODUCTO_ID INTEGER,
                $COL_HIST_PRECIO REAL NOT NULL,
                $COL_HIST_FECHA TEXT NOT NULL,
                $COL_HIST_TIENDA TEXT NOT NULL,
                FOREIGN KEY($COL_HIST_PRODUCTO_ID) REFERENCES $TABLE_PRODUCTOS($COL_ID)
            )
        """.trimIndent()

        db.execSQL(createProductosTable)
        db.execSQL(createHistorialTable)
        val createAlertasTable = """
        CREATE TABLE $TABLE_ALERTAS (
            $COL_ALERT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_ALERT_PRODUCTO_NOMBRE TEXT NOT NULL,
            $COL_ALERT_PRECIO_OBJETIVO REAL NOT NULL,
            $COL_ALERT_ACTIVA INTEGER NOT NULL DEFAULT 1,
            $COL_ALERT_FECHA_CREACION TEXT NOT NULL,
            $COL_ALERT_TIPO TEXT NOT NULL,
            $COL_ALERT_USUARIO_ID TEXT
        )
    """.trimIndent()

        // Tabla de eventos del sistema
        val createEventosTable = """
        CREATE TABLE $TABLE_EVENTOS (
            $COL_EVENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_EVENT_TIPO TEXT NOT NULL,
            $COL_EVENT_PRODUCTO_ID INTEGER,
            $COL_EVENT_DATOS TEXT,
            $COL_EVENT_FECHA TEXT NOT NULL,
            $COL_EVENT_PROCESADO INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY($COL_EVENT_PRODUCTO_ID) REFERENCES $TABLE_PRODUCTOS($COL_ID)
        )
    """.trimIndent()

        // Tabla de reglas de negocio
        val createReglasTable = """
        CREATE TABLE $TABLE_REGLAS (
            $COL_REGLA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_REGLA_NOMBRE TEXT NOT NULL,
            $COL_REGLA_CONDICIONES TEXT NOT NULL,
            $COL_REGLA_ACCION TEXT NOT NULL,
            $COL_REGLA_ACTIVA INTEGER NOT NULL DEFAULT 1,
            $COL_REGLA_PRIORIDAD INTEGER NOT NULL DEFAULT 1
        )
    """.trimIndent()

        // Tabla de notificaciones
        val createNotificacionesTable = """
        CREATE TABLE $TABLE_NOTIFICACIONES (
            $COL_NOTIF_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_NOTIF_TITULO TEXT NOT NULL,
            $COL_NOTIF_MENSAJE TEXT NOT NULL,
            $COL_NOTIF_TIPO TEXT NOT NULL,
            $COL_NOTIF_FECHA TEXT NOT NULL,
            $COL_NOTIF_LEIDA INTEGER NOT NULL DEFAULT 0,
            $COL_NOTIF_PRODUCTO_ID INTEGER,
            FOREIGN KEY($COL_NOTIF_PRODUCTO_ID) REFERENCES $TABLE_PRODUCTOS($COL_ID)
        )
    """.trimIndent()

        // Tabla de métricas
        val createMetricasTable = """
        CREATE TABLE $TABLE_METRICAS (
            $COL_METRIC_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_METRIC_NOMBRE TEXT NOT NULL,
            $COL_METRIC_VALOR REAL NOT NULL,
            $COL_METRIC_FECHA TEXT NOT NULL,
            $COL_METRIC_CATEGORIA TEXT NOT NULL
        )
    """.trimIndent()

        db.execSQL(createAlertasTable)
        db.execSQL(createEventosTable)
        db.execSQL(createReglasTable)
        db.execSQL(createNotificacionesTable)
        db.execSQL(createMetricasTable)

        // Crear índices para mejorar performance
        db.execSQL("CREATE INDEX idx_eventos_fecha ON $TABLE_EVENTOS($COL_EVENT_FECHA)")
        db.execSQL("CREATE INDEX idx_eventos_procesado ON $TABLE_EVENTOS($COL_EVENT_PROCESADO)")
        db.execSQL("CREATE INDEX idx_alertas_activa ON $TABLE_ALERTAS($COL_ALERT_ACTIVA)")
        db.execSQL("CREATE INDEX idx_historial_fecha ON $TABLE_HISTORIAL($COL_HIST_FECHA)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORIAL")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTOS")
        onCreate(db)
    }

    // Insertar o actualizar producto
    fun insertarOActualizarProducto(producto: ProductoScraping): Long {
        val db = this.writableDatabase
        val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Buscar si el producto ya existe
        val cursor = db.query(
            TABLE_PRODUCTOS,
            arrayOf(COL_ID, COL_PRECIO),
            "$COL_NOMBRE = ? AND $COL_TIENDA = ?",
            arrayOf(producto.nombre, producto.tienda),
            null, null, null
        )

        var productoId: Long = -1

        if (cursor.moveToFirst()) {
            // Producto existe, actualizar
            productoId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))
            val precioAnterior = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PRECIO))

            val values = ContentValues().apply {
                put(COL_PRECIO, producto.precio)
                put(COL_DISPONIBLE, if (producto.disponible) 1 else 0)
                put(COL_FECHA_REGISTRO, fechaActual)
            }

            db.update(TABLE_PRODUCTOS, values, "$COL_ID = ?", arrayOf(productoId.toString()))

            // Si el precio cambió, agregar al historial
            if (precioAnterior != producto.precio) {
                insertarHistorialPrecio(productoId, producto.precio, producto.tienda)
            }
        } else {
            // Producto nuevo, insertar
            val values = ContentValues().apply {
                put(COL_NOMBRE, producto.nombre)
                put(COL_PRECIO, producto.precio)
                put(COL_TIENDA, producto.tienda)
                put(COL_DISPONIBLE, if (producto.disponible) 1 else 0)
                put(COL_FECHA_REGISTRO, fechaActual)
                put(COL_CATEGORIA, determinarCategoria(producto.nombre))
                put(COL_URL, producto.url ?: "")
            }

            productoId = db.insert(TABLE_PRODUCTOS, null, values)

            // Insertar precio inicial en historial
            insertarHistorialPrecio(productoId, producto.precio, producto.tienda)
        }

        cursor.close()
        db.close()
        return productoId
    }


    // Insertar historial de precio
    private fun insertarHistorialPrecio(productoId: Long, precio: Double, tienda: String) {
        val db = this.writableDatabase
        val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val values = ContentValues().apply {
            put(COL_HIST_PRODUCTO_ID, productoId)
            put(COL_HIST_PRECIO, precio)
            put(COL_HIST_FECHA, fechaActual)
            put(COL_HIST_TIENDA, tienda)
        }

        db.insert(TABLE_HISTORIAL, null, values)
        db.close()
    }

    // Obtener historial de precios de un producto
    fun obtenerHistorialPrecios(nombreProducto: String, tienda: String): List<HistorialPrecio> {
        val historial = mutableListOf<HistorialPrecio>()
        val db = this.readableDatabase

        val query = """
            SELECT h.$COL_HIST_PRECIO, h.$COL_HIST_FECHA, h.$COL_HIST_TIENDA
            FROM $TABLE_HISTORIAL h
            INNER JOIN $TABLE_PRODUCTOS p ON h.$COL_HIST_PRODUCTO_ID = p.$COL_ID
            WHERE p.$COL_NOMBRE = ? AND p.$COL_TIENDA = ?
            ORDER BY h.$COL_HIST_FECHA DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(nombreProducto, tienda))

        while (cursor.moveToNext()) {
            val precio = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_HIST_PRECIO))
            val fecha = cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_FECHA))
            val tiendaHist = cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_TIENDA))

            historial.add(HistorialPrecio(precio, fecha, tiendaHist))
        }

        cursor.close()
        db.close()
        return historial
    }

    // Obtener estadísticas de precios
    fun obtenerEstadisticasPrecios(nombreProducto: String): EstadisticasPrecio? {
        val db = this.readableDatabase

        val query = """
            SELECT 
                MIN(h.$COL_HIST_PRECIO) as precio_min,
                MAX(h.$COL_HIST_PRECIO) as precio_max,
                AVG(h.$COL_HIST_PRECIO) as precio_promedio,
                COUNT(*) as total_registros
            FROM $TABLE_HISTORIAL h
            INNER JOIN $TABLE_PRODUCTOS p ON h.$COL_HIST_PRODUCTO_ID = p.$COL_ID
            WHERE p.$COL_NOMBRE LIKE ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf("%$nombreProducto%"))

        var estadisticas: EstadisticasPrecio? = null

        if (cursor.moveToFirst()) {
            val precioMin = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_min"))
            val precioMax = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_max"))
            val precioPromedio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_promedio"))
            val totalRegistros = cursor.getInt(cursor.getColumnIndexOrThrow("total_registros"))

            estadisticas = EstadisticasPrecio(precioMin, precioMax, precioPromedio, totalRegistros)
        }

        cursor.close()
        db.close()
        return estadisticas
    }

    // Obtener tendencia de precios (últimos 30 días)
    fun obtenerTendenciaPrecios(nombreProducto: String): List<PuntoTendencia> {
        val tendencia = mutableListOf<PuntoTendencia>()
        val db = this.readableDatabase

        val query = """
            SELECT 
                DATE(h.$COL_HIST_FECHA) as fecha,
                AVG(h.$COL_HIST_PRECIO) as precio_promedio
            FROM $TABLE_HISTORIAL h
            INNER JOIN $TABLE_PRODUCTOS p ON h.$COL_HIST_PRODUCTO_ID = p.$COL_ID
            WHERE p.$COL_NOMBRE LIKE ? 
            AND h.$COL_HIST_FECHA >= datetime('now', '-30 days')
            GROUP BY DATE(h.$COL_HIST_FECHA)
            ORDER BY fecha ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf("%$nombreProducto%"))

        while (cursor.moveToNext()) {
            val fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha"))
            val precioPromedio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_promedio"))

            tendencia.add(PuntoTendencia(fecha, precioPromedio))
        }

        cursor.close()
        db.close()
        return tendencia
    }

    // Obtener productos con mejor precio histórico
    fun obtenerMejoresOportunidades(): List<OportunidadCompra> {
        val oportunidades = mutableListOf<OportunidadCompra>()
        val db = this.readableDatabase

        val query = """
            SELECT 
                p.$COL_NOMBRE,
                p.$COL_PRECIO as precio_actual,
                p.$COL_TIENDA,
                MIN(h.$COL_HIST_PRECIO) as precio_minimo,
                MAX(h.$COL_HIST_PRECIO) as precio_maximo,
                AVG(h.$COL_HIST_PRECIO) as precio_promedio
            FROM $TABLE_PRODUCTOS p
            INNER JOIN $TABLE_HISTORIAL h ON p.$COL_ID = h.$COL_HIST_PRODUCTO_ID
            WHERE p.$COL_DISPONIBLE = 1
            GROUP BY p.$COL_ID
            HAVING COUNT(h.$COL_HIST_ID) >= 3
            ORDER BY (p.$COL_PRECIO - MIN(h.$COL_HIST_PRECIO)) ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val nombre = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOMBRE))
            val precioActual = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_actual"))
            val tienda = cursor.getString(cursor.getColumnIndexOrThrow(COL_TIENDA))
            val precioMinimo = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_minimo"))
            val precioMaximo = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_maximo"))
            val precioPromedio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_promedio"))

            val porcentajeDescuento = ((precioPromedio - precioActual) / precioPromedio) * 100

            oportunidades.add(
                OportunidadCompra(
                    nombre, precioActual, tienda, precioMinimo,
                    precioMaximo, precioPromedio, porcentajeDescuento
                )
            )
        }

        cursor.close()
        db.close()
        return oportunidades.take(10) // Top 10 oportunidades
    }

    private fun determinarCategoria(nombre: String): String {
        val nombreLower = nombre.lowercase()
        return when {
            nombreLower.contains("iphone") -> "iPhone"
            nombreLower.contains("samsung") -> "Samsung"
            nombreLower.contains("huawei") -> "Huawei"
            nombreLower.contains("xiaomi") -> "Xiaomi"
            nombreLower.contains("laptop") -> "Laptop"
            nombreLower.contains("tablet") -> "Tablet"
            else -> "Otros"
        }
    }
    fun insertarEvento(tipo: String, productoId: Long?, datos: String? = null): Long {
        val db = this.writableDatabase
        val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val values = ContentValues().apply {
            put(COL_EVENT_TIPO, tipo)
            put(COL_EVENT_PRODUCTO_ID, productoId)
            put(COL_EVENT_DATOS, datos)
            put(COL_EVENT_FECHA, fechaActual)
            put(COL_EVENT_PROCESADO, 0)
        }

        val eventoId = db.insert(TABLE_EVENTOS, null, values)
        db.close()
        return eventoId
    }

    fun obtenerEventosNoProcesados(): List<EventoSistema> {
        val eventos = mutableListOf<EventoSistema>()
        val db = this.readableDatabase

        val cursor = db.query(
            TABLE_EVENTOS,
            null,
            "$COL_EVENT_PROCESADO = ?",
            arrayOf("0"),
            null, null,
            "$COL_EVENT_FECHA ASC"
        )

        while (cursor.moveToNext()) {
            val evento = EventoSistema(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EVENT_ID)),
                tipo = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_TIPO)),
                productoId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EVENT_PRODUCTO_ID)),
                datos = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_DATOS)),
                fecha = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_FECHA)),
                procesado = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_PROCESADO)) == 1
            )
            eventos.add(evento)
        }

        cursor.close()
        db.close()
        return eventos
    }

    fun marcarEventoComoProcesado(eventoId: Long) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_EVENT_PROCESADO, 1)
        }

        db.update(TABLE_EVENTOS, values, "$COL_EVENT_ID = ?", arrayOf(eventoId.toString()))
        db.close()
    }

    // 4. Funciones para manejo de ALERTAS (Programación reactiva)
    fun insertarAlerta(alerta: AlertaUsuario): Long {
        val db = this.writableDatabase
        val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val values = ContentValues().apply {
            put(COL_ALERT_PRODUCTO_NOMBRE, alerta.productoNombre)
            put(COL_ALERT_PRECIO_OBJETIVO, alerta.precioObjetivo)
            put(COL_ALERT_ACTIVA, if (alerta.activa) 1 else 0)
            put(COL_ALERT_FECHA_CREACION, fechaActual)
            put(COL_ALERT_TIPO, alerta.tipo)
            put(COL_ALERT_USUARIO_ID, alerta.usuarioId)
        }

        val alertaId = db.insert(TABLE_ALERTAS, null, values)
        db.close()
        return alertaId
    }

    fun obtenerAlertasActivas(): List<AlertaUsuario> {
        val alertas = mutableListOf<AlertaUsuario>()
        val db = this.readableDatabase

        val cursor = db.query(
            TABLE_ALERTAS,
            null,
            "$COL_ALERT_ACTIVA = ?",
            arrayOf("1"),
            null, null,
            "$COL_ALERT_FECHA_CREACION DESC"
        )

        while (cursor.moveToNext()) {
            val alerta = AlertaUsuario(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ALERT_ID)),
                productoNombre = cursor.getString(cursor.getColumnIndexOrThrow(COL_ALERT_PRODUCTO_NOMBRE)),
                precioObjetivo = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ALERT_PRECIO_OBJETIVO)),
                activa = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ALERT_ACTIVA)) == 1,
                fechaCreacion = cursor.getString(cursor.getColumnIndexOrThrow(COL_ALERT_FECHA_CREACION)),
                tipo = cursor.getString(cursor.getColumnIndexOrThrow(COL_ALERT_TIPO)),
                usuarioId = cursor.getString(cursor.getColumnIndexOrThrow(COL_ALERT_USUARIO_ID))
            )
            alertas.add(alerta)
        }

        cursor.close()
        db.close()
        return alertas
    }

    // 5. Funciones para REGLAS DE NEGOCIO (Programación lógica)
    fun insertarRegla(regla: ReglaNegocio): Long {
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put(COL_REGLA_NOMBRE, regla.nombre)
            put(COL_REGLA_CONDICIONES, regla.condicionesJson)
            put(COL_REGLA_ACCION, regla.accionJson)
            put(COL_REGLA_ACTIVA, if (regla.activa) 1 else 0)
            put(COL_REGLA_PRIORIDAD, regla.prioridad)
        }

        val reglaId = db.insert(TABLE_REGLAS, null, values)
        db.close()
        return reglaId
    }

    fun obtenerReglasActivas(): List<ReglaNegocio> {
        val reglas = mutableListOf<ReglaNegocio>()
        val db = this.readableDatabase

        val cursor = db.query(
            TABLE_REGLAS,
            null,
            "$COL_REGLA_ACTIVA = ?",
            arrayOf("1"),
            null, null,
            "$COL_REGLA_PRIORIDAD DESC"
        )

        while (cursor.moveToNext()) {
            val regla = ReglaNegocio(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_REGLA_ID)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(COL_REGLA_NOMBRE)),
                condicionesJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_REGLA_CONDICIONES)),
                accionJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_REGLA_ACCION)),
                activa = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REGLA_ACTIVA)) == 1,
                prioridad = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REGLA_PRIORIDAD))
            )
            reglas.add(regla)
        }

        cursor.close()
        db.close()
        return reglas
    }

    // 6. Funciones para NOTIFICACIONES (Sistema reactivo)
    fun insertarNotificacion(notificacion: Notificacion): Long {
        val db = this.writableDatabase
        val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val values = ContentValues().apply {
            put(COL_NOTIF_TITULO, notificacion.titulo)
            put(COL_NOTIF_MENSAJE, notificacion.mensaje)
            put(COL_NOTIF_TIPO, notificacion.tipo)
            put(COL_NOTIF_FECHA, fechaActual)
            put(COL_NOTIF_LEIDA, 0)
            put(COL_NOTIF_PRODUCTO_ID, notificacion.productoId)
        }

        val notifId = db.insert(TABLE_NOTIFICACIONES, null, values)
        db.close()
        return notifId
    }

    fun obtenerNotificacionesNoLeidas(): List<Notificacion> {
        val notificaciones = mutableListOf<Notificacion>()
        val db = this.readableDatabase

        val cursor = db.query(
            TABLE_NOTIFICACIONES,
            null,
            "$COL_NOTIF_LEIDA = ?",
            arrayOf("0"),
            null, null,
            "$COL_NOTIF_FECHA DESC"
        )

        while (cursor.moveToNext()) {
            val notificacion = Notificacion(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_NOTIF_ID)),
                titulo = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_TITULO)),
                mensaje = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_MENSAJE)),
                tipo = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_TIPO)),
                fecha = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_FECHA)),
                leida = cursor.getInt(cursor.getColumnIndexOrThrow(COL_NOTIF_LEIDA)) == 1,
                productoId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_NOTIF_PRODUCTO_ID))
            )
            notificaciones.add(notificacion)
        }

        cursor.close()
        db.close()
        return notificaciones
    }

    // 7. Funciones para MÉTRICAS (Analytics)
    fun insertarMetrica(nombre: String, valor: Double, categoria: String) {
        val db = this.writableDatabase
        val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val values = ContentValues().apply {
            put(COL_METRIC_NOMBRE, nombre)
            put(COL_METRIC_VALOR, valor)
            put(COL_METRIC_FECHA, fechaActual)
            put(COL_METRIC_CATEGORIA, categoria)
        }

        db.insert(TABLE_METRICAS, null, values)
        db.close()
    }

    fun obtenerMetricasCategoria(categoria: String, limiteDias: Int = 30): List<Metrica> {
        val metricas = mutableListOf<Metrica>()
        val db = this.readableDatabase

        val cursor = db.query(
            TABLE_METRICAS,
            null,
            "$COL_METRIC_CATEGORIA = ? AND $COL_METRIC_FECHA >= datetime('now', '-$limiteDias days')",
            arrayOf(categoria),
            null, null,
            "$COL_METRIC_FECHA DESC"
        )

        while (cursor.moveToNext()) {
            val metrica = Metrica(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_METRIC_ID)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(COL_METRIC_NOMBRE)),
                valor = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_METRIC_VALOR)),
                fecha = cursor.getString(cursor.getColumnIndexOrThrow(COL_METRIC_FECHA)),
                categoria = cursor.getString(cursor.getColumnIndexOrThrow(COL_METRIC_CATEGORIA))
            )
            metricas.add(metrica)
        }

        cursor.close()
        db.close()
        return metricas
    }

    // 8. Función para detectar cambios de precio (Trigger de eventos)
    fun verificarYDispararEventos(nombreProducto: String, precioAnterior: Double, precioNuevo: Double) {
        val diferencia = precioAnterior - precioNuevo
        val porcentajeCambio = (diferencia / precioAnterior) * 100

        // Disparar evento si hay cambio significativo
        if (Math.abs(porcentajeCambio) > 5) {
            val tipoEvento = if (diferencia > 0) "PRECIO_BAJO" else "PRECIO_ALTO"
            val datosEvento = """
            {
                "precio_anterior": $precioAnterior,
                "precio_nuevo": $precioNuevo,
                "diferencia": $diferencia,
                "porcentaje_cambio": $porcentajeCambio
            }
        """.trimIndent()

            insertarEvento(tipoEvento, null, datosEvento)
        }
    }
    fun obtenerProductosConAlertasActivas(): List<ProductoConAlerta> {
        val productos = mutableListOf<ProductoConAlerta>()
        val db = this.readableDatabase

        val query = """
            SELECT 
                p.$COL_ID,
                p.$COL_NOMBRE,
                p.$COL_PRECIO,
                p.$COL_TIENDA,
                p.$COL_URL,
                a.$COL_ALERT_PRECIO_OBJETIVO,
                a.$COL_ALERT_TIPO,
                a.$COL_ALERT_ID
            FROM $TABLE_PRODUCTOS p
            INNER JOIN $TABLE_ALERTAS a ON p.$COL_NOMBRE = a.$COL_ALERT_PRODUCTO_NOMBRE
            WHERE p.$COL_DISPONIBLE = 1 
            AND a.$COL_ALERT_ACTIVA = 1
        """

        val cursor = db.rawQuery(query, null)
        while (cursor.moveToNext()) {
            val producto = ProductoConAlerta(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOMBRE)),
                precioActual = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PRECIO)),
                tienda = cursor.getString(cursor.getColumnIndexOrThrow(COL_TIENDA)),
                url = cursor.getString(cursor.getColumnIndexOrThrow(COL_URL)),
                precioObjetivo = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ALERT_PRECIO_OBJETIVO)),
                tipoAlerta = cursor.getString(cursor.getColumnIndexOrThrow(COL_ALERT_TIPO)),
                alertaId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ALERT_ID))
            )
            productos.add(producto)
        }
        cursor.close()
        return productos
    }

    // ✅ Método para buscar productos con RxJava
    fun buscarProductosRx(query: String): Single<List<Producto>> {
        return Single.fromCallable {
            buscarProductos(query)
        }.subscribeOn(Schedulers.io())
    }

    private fun buscarProductos(query: String): List<Producto> {
        val productos = mutableListOf<Producto>()
        val db = this.readableDatabase

        val sqlQuery = """
            SELECT * FROM $TABLE_PRODUCTOS 
            WHERE ($COL_NOMBRE LIKE ? OR $COL_CATEGORIA LIKE ?)
            AND $COL_DISPONIBLE = 1
            ORDER BY $COL_PRECIO ASC
        """

        val cursor = db.rawQuery(sqlQuery, arrayOf("%$query%", "%$query%"))
        while (cursor.moveToNext()) {
            val producto = Producto(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOMBRE)),
                precio = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PRECIO)),
                tienda = cursor.getString(cursor.getColumnIndexOrThrow(COL_TIENDA)),
                categoria = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORIA)),
                url = cursor.getString(cursor.getColumnIndexOrThrow(COL_URL))
            )
            productos.add(producto)
        }
        cursor.close()
        return productos
    }

    // ✅ Crear alerta para un producto
    fun crearAlerta(productoNombre: String, precioObjetivo: Double, tipo: String = "PRECIO_BAJO"): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_ALERT_PRODUCTO_NOMBRE, productoNombre)
            put(COL_ALERT_PRECIO_OBJETIVO, precioObjetivo)
            put(COL_ALERT_ACTIVA, 1)
            put(COL_ALERT_FECHA_CREACION, System.currentTimeMillis())
            put(COL_ALERT_TIPO, tipo)
            put(COL_ALERT_USUARIO_ID, 1) // Por defecto usuario 1
        }

        return db.insert(TABLE_ALERTAS, null, values)
    }

    // ✅ Actualizar precio y registrar en historial
    fun actualizarPrecioConHistorial(productoId: Int, nuevoPrecio: Double, tienda: String): Boolean {
        val db = this.writableDatabase

        // Actualizar precio en tabla productos
        val values = ContentValues().apply {
            put(COL_PRECIO, nuevoPrecio)
        }

        val rowsUpdated = db.update(
            TABLE_PRODUCTOS,
            values,
            "$COL_ID = ?",
            arrayOf(productoId.toString())
        )

        // Insertar en historial
        if (rowsUpdated > 0) {
            val historialValues = ContentValues().apply {
                put(COL_HIST_PRODUCTO_ID, productoId)
                put(COL_HIST_PRECIO, nuevoPrecio)
                put(COL_HIST_FECHA, System.currentTimeMillis())
                put(COL_HIST_TIENDA, tienda)
            }
            db.insert(TABLE_HISTORIAL, null, historialValues)
        }

        return rowsUpdated > 0
    }

    // ✅ Crear notificación
    fun crearNotificacion(titulo: String, mensaje: String, tipo: String, productoId: Int): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_NOTIF_TITULO, titulo)
            put(COL_NOTIF_MENSAJE, mensaje)
            put(COL_NOTIF_TIPO, tipo)
            put(COL_NOTIF_FECHA, System.currentTimeMillis())
            put(COL_NOTIF_LEIDA, 0)
            put(COL_NOTIF_PRODUCTO_ID, productoId)
        }

        return db.insert(TABLE_NOTIFICACIONES, null, values)
    }

    // ✅ Desactivar alerta
    fun desactivarAlerta(alertaId: Int): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_ALERT_ACTIVA, 0)
        }

        val rowsUpdated = db.update(
            TABLE_ALERTAS,
            values,
            "$COL_ALERT_ID = ?",
            arrayOf(alertaId.toString())
        )

        return rowsUpdated > 0
    }

    // ✅ Registrar evento del sistema
    fun registrarEvento(tipo: String, productoId: Int, datos: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_EVENT_TIPO, tipo)
            put(COL_EVENT_PRODUCTO_ID, productoId)
            put(COL_EVENT_DATOS, datos)
            put(COL_EVENT_FECHA, System.currentTimeMillis())
            put(COL_EVENT_PROCESADO, 0)
        }

        return db.insert(TABLE_EVENTOS, null, values)
    }

    // ✅ Registrar métrica
    fun registrarMetrica(nombre: String, valor: String, categoria: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_METRIC_NOMBRE, nombre)
            put(COL_METRIC_VALOR, valor)
            put(COL_METRIC_FECHA, System.currentTimeMillis())
            put(COL_METRIC_CATEGORIA, categoria)
        }

        return db.insert(TABLE_METRICAS, null, values)
    }
}

// Clases de datos para el análisis
data class HistorialPrecio(
    val precio: Double,
    val fecha: String,
    val tienda: String
)

data class EstadisticasPrecio(
    val precioMinimo: Double,
    val precioMaximo: Double,
    val precioPromedio: Double,
    val totalRegistros: Int
)

data class PuntoTendencia(
    val fecha: String,
    val precio: Double
)

data class OportunidadCompra(
    val nombre: String,
    val precioActual: Double,
    val tienda: String,
    val precioMinimo: Double,
    val precioMaximo: Double,
    val precioPromedio: Double,
    val porcentajeDescuento: Double
)
data class EventoSistema(
    val id: Long,
    val tipo: String,
    val productoId: Long,
    val datos: String?,
    val fecha: String,
    val procesado: Boolean
)

data class AlertaUsuario(
    val id: Long = 0,
    val productoNombre: String,
    val precioObjetivo: Double,
    val activa: Boolean,
    val fechaCreacion: String = "",
    val tipo: String, // "PRECIO_BAJO", "DISPONIBILIDAD", "DESCUENTO"
    val usuarioId: String?
)

data class ReglaNegocio(
    val id: Long = 0,
    val nombre: String,
    val condicionesJson: String,
    val accionJson: String,
    val activa: Boolean,
    val prioridad: Int
)

data class Notificacion(
    val id: Long = 0,
    val titulo: String,
    val mensaje: String,
    val tipo: String,
    val fecha: String = "",
    val leida: Boolean = false,
    val productoId: Long?
)

data class Metrica(
    val id: Long,
    val nombre: String,
    val valor: Double,
    val fecha: String,
    val categoria: String
)
// ✅ Clases de datos actualizadas
data class Producto(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val tienda: String,
    val categoria: String,
    val url: String
)

data class ProductoConAlerta(
    val id: Int,
    val nombre: String,
    val precioActual: Double,
    val tienda: String,
    val url: String,
    val precioObjetivo: Double,
    val tipoAlerta: String,
    val alertaId: Int
)

data class MejorPrecioEncontrado(
    val producto: ProductoConAlerta,
    val precioAnterior: Double,
    val precioNuevo: Double,
    val ahorro: Double = precioAnterior - precioNuevo
)