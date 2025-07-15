package com.example.lenguajes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ScrapingViewModel : ViewModel() {
    private val repository = ScrapingRepository()

    private val _productos = MutableLiveData<List<ProductoScraping>>()
    val productos: LiveData<List<ProductoScraping>> = _productos

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun buscarProductos(query: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            repository.buscarProducto(query)
                .onSuccess { productos ->
                    _productos.value = productos
                    _error.value = null // ✅ Asegura que se borre el error al tener éxito
                    _loading.value = false
                }
                .onFailure { exception ->
                    _error.value = exception.message
                    _loading.value = false
                }
        }
    }

    fun obtenerMejorPrecio(): ProductoScraping? {
        return _productos.value?.minByOrNull { it.precio }
    }

    fun obtenerPromedioPrecio(): Double {
        val productos = _productos.value ?: return 0.0
        return if (productos.isNotEmpty()) {
            productos.map { it.precio }.average()
        } else 0.0
    }
}
