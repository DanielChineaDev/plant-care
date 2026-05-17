package com.BPO.plantcare.ui.screens.photoviewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.PlantPhoto
import com.BPO.plantcare.domain.usecase.ObservePlantPhotosUseCase
import com.BPO.plantcare.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePhotos: ObservePlantPhotosUseCase,
) : ViewModel() {

    private val plantId: Long =
        checkNotNull(savedStateHandle.get<Long>(NavArgs.PLANT_ID))
    val initialPhotoId: Long =
        checkNotNull(savedStateHandle.get<Long>(NavArgs.PHOTO_ID))

    val photos: StateFlow<List<PlantPhoto>> = observePhotos(plantId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
}
