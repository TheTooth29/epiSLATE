package com.nuvio.tv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.data.local.ThemeDataStore
import com.nuvio.tv.domain.model.HomeDimmingSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeDimmingViewModel @Inject constructor(private val store: ThemeDataStore) : ViewModel() {
    val settings = store.homeDimming.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeDimmingSettings())
    fun save(settings: HomeDimmingSettings) { viewModelScope.launch { store.setHomeDimming(settings) } }
}
