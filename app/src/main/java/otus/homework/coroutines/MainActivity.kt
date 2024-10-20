package otus.homework.coroutines

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val diContainer = DiContainer()
    private var catView: CatsView? = null

    private val viewModelStoreOwner: ViewModelStoreOwner = this
    private val catsViewModel: CatsViewModel by lazy {
        ViewModelProvider.create(
            viewModelStoreOwner,
            factory = CatsViewModel.Factory,
            extras = MutableCreationExtras().apply {
                set(CatsViewModel.CATS_SERVICE, diContainer.factService)
                set(CatsViewModel.IMAGE_SERVICE, diContainer.imageService)
            },
        )[CatsViewModel::class]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        catView = layoutInflater.inflate(R.layout.activity_main, null) as CatsView
        setContentView(catView)

        initObservers()
        catView?.setOnButtonClickListener {
            catsViewModel.getCats()
        }
        catsViewModel.getCats()
    }

    override fun onStop() {
        catsViewModel.cancelJob()
        super.onStop()
    }

    private fun initObservers() {
        lifecycleScope.launch {
            launch {
                catsViewModel.catState.collect { catView?.populate(it) }
            }
            launch {
                catsViewModel.eventShowErrorConnectToServer.collect { showErrorToast() }
            }
            launch {
                catsViewModel.eventShowExceptionMessage.collect(::showErrorToast)
            }
        }
    }

    private fun showErrorToast(message: String? = null) = Toast
        .makeText(this, message ?: getString(R.string.error_to_connect_message), Toast.LENGTH_SHORT)
        .show()
}