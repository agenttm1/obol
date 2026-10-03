package com.tmstudio.obol.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tmstudio.obol.ObolApplication
import com.tmstudio.obol.data.db.dao.ServiceDao
import com.tmstudio.obol.data.db.entity.Service
import com.tmstudio.obol.domain.search
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AddUiState(
    val query: String = "",
    val catalogSize: Int = 0,
    /** Bez upita: popularni servisi, pa ostali. */
    val popular: List<Service> = emptyList(),
    val others: List<Service> = emptyList(),
    /** S upitom: rezultati pretrage; null kad upita nema. */
    val results: List<Service>? = null,
)

/**
 * Redoslijed mreže „Popularno" iz mockupa `Dodaj.html`. Servisi koji nisu
 * ovdje idu u „Ostali servisi", po abecedi.
 */
internal val POPULAR_SERVICE_IDS = listOf(
    "netflix", "spotify", "hbo-max", "disney-plus", "youtube-premium", "playstation-plus",
    "xbox-game-pass", "icloud-plus", "google-one", "microsoft-365", "duolingo", "nintendo-switch-online",
)

class AddViewModel(serviceDao: ServiceDao) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<AddUiState> = combine(serviceDao.observeAll(), query) { services, query ->
        val byId = services.associateBy { it.id }
        val popular = POPULAR_SERVICE_IDS.mapNotNull(byId::get)
        val popularIds = popular.map { it.id }.toSet()
        AddUiState(
            query = query,
            catalogSize = services.size,
            popular = popular,
            others = services.filterNot { it.id in popularIds },
            results = if (query.isBlank()) null else services.search(query),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddUiState())

    val queryText: StateFlow<String> = query.asStateFlow()

    fun onQueryChange(text: String) {
        query.value = text
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ObolApplication
                AddViewModel(app.container.database.serviceDao())
            }
        }
    }
}
