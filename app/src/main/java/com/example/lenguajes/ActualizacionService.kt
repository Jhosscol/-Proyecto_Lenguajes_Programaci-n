package com.example.lenguajes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

// Servicio de Actualización Automática adaptado a tu estructura
class ActualizacionService : Service() {
    private val disposables = CompositeDisposable()
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        databaseHelper = DatabaseHelper(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificaciones
        crearCanalNotificaciones()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        iniciarActualizacionAutomatica()
        return START_STICKY
    }

    private fun iniciarActualizacionAutomatica() {
        // Actualización cada 24 horas
        val subscription = Observable.interval(0, 24, TimeUnit.HOURS)
            .observeOn(Schedulers.io())
            .flatMap {
                verificarAlertasActivas()
                    .toObservable()
                    .onErrorReturn { error ->
                        // Registrar error como evento
                        databaseHelper.registrarEvento(
                            "ERROR_ACTUALIZACION",
                            0,
                            "{\"error\": \"${error.message}\", \"timestamp\": ${System.currentTimeMillis()}}"
                        )
                        emptyList()
                    }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { alertasActivadas ->
                if (alertasActivadas.isNotEmpty()) {
                    procesarAlertasActivadas(alertasActivadas)
                }

                // Registrar métrica de verificación
                databaseHelper.registrarMetrica(
                    "verificacion_automatica",
                    "alertas_encontradas:${alertasActivadas.size}",
                    "sistema"
                )
            }

        disposables.add(subscription)
    }

    private fun verificarAlertasActivas(): Single<List<MejorPrecioEncontrado>> {
        return Single.fromCallable {
            val productosConAlertas = databaseHelper.obtenerProductosConAlertasActivas()
            val alertasActivadas = mutableListOf<MejorPrecioEncontrado>()

            for (producto in productosConAlertas) {
                try {
                    // Simular búsqueda de precio actualizado
                    // Aquí implementarías la lógica real de web scraping o API
                    val precioActualizado = buscarPrecioActualizado(producto)

                    if (precioActualizado != null) {
                        // Actualizar precio en base de datos
                        databaseHelper.actualizarPrecioConHistorial(
                            producto.id,
                            precioActualizado,
                            producto.tienda
                        )

                        // Verificar si se cumple la condición de alerta
                        val alertaActivada = verificarCondicionAlerta(producto, precioActualizado)

                        if (alertaActivada != null) {
                            alertasActivadas.add(alertaActivada)

                            // Registrar evento de alerta activada
                            databaseHelper.registrarEvento(
                                "ALERTA_ACTIVADA",
                                producto.id,
                                "{\"precio_anterior\": ${producto.precioActual}, \"precio_nuevo\": $precioActualizado, \"alerta_id\": ${producto.alertaId}}"
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Registrar error específico del producto
                    databaseHelper.registrarEvento(
                        "ERROR_PRODUCTO",
                        producto.id,
                        "{\"error\": \"${e.message}\", \"producto\": \"${producto.nombre}\"}"
                    )
                }
            }

            alertasActivadas
        }
    }

    private fun buscarPrecioActualizado(producto: ProductoConAlerta): Double? {
        return try {
            // Aquí implementarías la lógica real de búsqueda
            // Por ejemplo, usando la URL del producto para hacer web scraping

            // Simulación - reemplaza con tu lógica real
            val precioSimulado = producto.precioActual * (0.8 + Math.random() * 0.4) // Variación del 20%

            // Registrar intento de búsqueda
            databaseHelper.registrarMetrica(
                "busqueda_precio",
                "producto_id:${producto.id}",
                "actualizacion"
            )

            precioSimulado
        } catch (e: Exception) {
            null
        }
    }

    private fun verificarCondicionAlerta(producto: ProductoConAlerta, precioActualizado: Double): MejorPrecioEncontrado? {
        return when (producto.tipoAlerta) {
            "PRECIO_BAJO" -> {
                if (precioActualizado <= producto.precioObjetivo) {
                    MejorPrecioEncontrado(
                        producto = producto,
                        precioAnterior = producto.precioActual,
                        precioNuevo = precioActualizado
                    )
                } else null
            }
            "DESCUENTO" -> {
                val porcentajeDescuento = ((producto.precioActual - precioActualizado) / producto.precioActual) * 100
                if (porcentajeDescuento >= producto.precioObjetivo) {
                    MejorPrecioEncontrado(
                        producto = producto,
                        precioAnterior = producto.precioActual,
                        precioNuevo = precioActualizado
                    )
                } else null
            }
            else -> null
        }
    }

    private fun procesarAlertasActivadas(alertasActivadas: List<MejorPrecioEncontrado>) {
        alertasActivadas.forEach { alerta ->
            // Crear notificación en base de datos
            val notificacionId = databaseHelper.crearNotificacion(
                titulo = "🎯 ¡Alerta de precio activada!",
                mensaje = "${alerta.producto.nombre} ahora cuesta $. ${String.format("%.2f", alerta.precioNuevo)}",
                tipo = "PRECIO_BAJO",
                productoId = alerta.producto.id
            )

            // Mostrar notificación del sistema
            mostrarNotificacionSistema(alerta)

            // Desactivar alerta para evitar spam
            databaseHelper.desactivarAlerta(alerta.producto.alertaId)
        }
    }

    private fun mostrarNotificacionSistema(alerta: MejorPrecioEncontrado) {
        val producto = alerta.producto

        val notification = NotificationCompat.Builder(this, "precio_alerts")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🎯 ${producto.nombre}")
            .setContentText("Precio objetivo alcanzado: $. ${String.format("%.2f", alerta.precioNuevo)}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("📦 ${producto.nombre}\n" +
                        "🏪 Tienda: ${producto.tienda}\n" +
                        "💰 Precio anterior: $. ${String.format("%.2f", alerta.precioAnterior)}\n" +
                        "🔥 Precio actual: $. ${String.format("%.2f", alerta.precioNuevo)}\n" +
                        "💸 Ahorras: $. ${String.format("%.2f", alerta.ahorro)}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(producto.id, notification)
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "precio_alerts",
                "Alertas de Precio",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando se alcanzan precios objetivo"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}

// Función para el botón "Crear Alerta"
fun crearAlertaPrecio(
    nombreProducto: String,
    precioObjetivo: Double,
    databaseHelper: DatabaseHelper, // 👈 lo pasas como parámetro
    context: Context
) {
    val alertaId = databaseHelper.crearAlerta(nombreProducto, precioObjetivo)

    if (alertaId > 0) {
        databaseHelper.registrarEvento(
            "ALERTA_CREADA",
            0,
            "{\"producto\": \"$nombreProducto\", \"precio_objetivo\": $precioObjetivo, \"alerta_id\": $alertaId}"
        )

        databaseHelper.registrarMetrica(
            "alerta_creada",
            "precio_objetivo:$precioObjetivo",
            "usuario"
        )

        Toast.makeText(context, "✅ Alerta creada para $nombreProducto", Toast.LENGTH_LONG).show()
    }
}
