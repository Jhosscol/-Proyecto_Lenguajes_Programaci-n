
# 📈 Lenguajes - Sistema Inteligente de Monitoreo y Predicción de Precios

**Lenguajes** es una app Android que permite optimizar decisiones de compra online mediante:
- Monitoreo automático de precios (scraping)
- Predicción de precios a corto plazo (regresión lineal)
- Alertas personalizadas con notificaciones inteligentes
- Motor lógico experto basado en reglas

## 🚀 Funcionalidades Clave

- 🔍 Búsqueda inteligente de productos con debounce
- 📊 Predicción de precios usando análisis histórico
- 🧠 Recomendaciones basadas en reglas lógicas configurables
- 📥 Alertas automáticas en segundo plano
- 📱 UI intuitiva para gestión de productos y análisis

## 🧪 Tecnologías Utilizadas

- Kotlin / Android Studio
- RxJava 3 / RxAndroid
- SQLite / Room
- Motor lógico personalizado (sistema experto)
- API de scraping REST (con clave)

## 🔑 Requisitos

> ⚠️ El sistema requiere una **API Key** gratuita para el scraping. Debes agregarla directamente en el archivo `ResApiRepository.kt`.

### Variables necesarias:
```kotlin
private val apiKey = "TU_API_KEY_AQUI"
```

## ⚙️ Instrucciones de Ejecución

1. Clona el repositorio:
   ```bash
   git clone https://github.com/tuusuario/lenguajes.git
   ```

2. Abre el proyecto en **Android Studio**.

3. Agrega tu `API_KEY` en el archivo `ResApiRepository.kt`.

4. Conecta un dispositivo Android o emulador.

5. Ejecuta el proyecto (Run ▶️).

## 📸 Capturas y Evidencias

Consulta el [Google Drive con evidencias](#) para:
- Screenshots de la app
- Videos de prueba de alertas
- APK para testeo

## 📄 Licencia

Proyecto académico sin fines de lucro. Uso libre para investigación y mejora personal. ¡Contribuciones bienvenidas!

