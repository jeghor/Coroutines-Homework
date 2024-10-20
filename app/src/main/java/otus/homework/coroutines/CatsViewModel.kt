package otus.homework.coroutines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CatsViewModel(
    private val catsService: CatsService,
    private val imageService: ImageService
): ViewModel() {

    companion object {

        val CATS_SERVICE = object : CreationExtras.Key<CatsService> {}
        val IMAGE_SERVICE = object : CreationExtras.Key<ImageService> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val catsService = this[CATS_SERVICE] as CatsService
                val imageService = this[IMAGE_SERVICE] as ImageService
                CatsViewModel(
                    catsService = catsService,
                    imageService = imageService
                )
            }
        }
    }

    private var catsJob: Job? = null

    private val _fact = MutableStateFlow<Fact?>(null)
    private val _image = MutableStateFlow<CatImage?>(null)

    val catState: Flow<Cat> = combine(_fact, _image) { fact, image -> Cat(fact?.fact, image?.url) }

    private val _eventShowErrorConnectToServer = MutableSharedFlow<Unit>()
    val eventShowErrorConnectToServer = _eventShowErrorConnectToServer.asSharedFlow()

    private val _eventShowExceptionMessage = MutableSharedFlow<String>()
    val eventShowExceptionMessage = _eventShowExceptionMessage.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        viewModelScope.launch {
            handleError(exception)
        }
    }

    fun getCats() {
        catsJob?.cancel()

        catsJob = viewModelScope.launch(exceptionHandler) {
            getRandomFact()
            getRandomImage()
        }
    }

    private suspend fun getRandomFact() {
        when (val result = catsService.getCatFact().handleApi()) {
            is NetworkResult.Success -> _fact.emit(result.data)
            is NetworkResult.Error -> CrashMonitor.trackApiError(result.error)
            is NetworkResult.Exception -> throw result.e
        }
    }

    private suspend fun getRandomImage() {
        when (val result = imageService.getRandomImages().handleApi()) {
            is NetworkResult.Success -> _image.emit(result.data.first())
            is NetworkResult.Error -> CrashMonitor.trackApiError(result.error)
            is NetworkResult.Exception -> throw result.e
        }
    }

    private suspend fun handleError(error: Throwable) {
        when (error) {
            is java.net.SocketTimeoutException -> {
                _eventShowErrorConnectToServer.emit(Unit)
            }

            else -> {
                CrashMonitor.trackWarning(error)
                error.message?.let { _eventShowExceptionMessage.emit(it) }
            }
        }
    }

    fun cancelJob() {
        catsJob?.cancel()
    }
}