package com.rafaelswitala.mediguard.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafaelswitala.mediguard.domain.model.ContactCategory
import com.rafaelswitala.mediguard.domain.model.DocumentCategory
import com.rafaelswitala.mediguard.domain.model.HealthAllergy
import com.rafaelswitala.mediguard.domain.model.HealthContact
import com.rafaelswitala.mediguard.domain.model.HealthProfile
import com.rafaelswitala.mediguard.domain.model.MedicalDocument
import com.rafaelswitala.mediguard.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(ProfileUiState())
    val uiState: androidx.compose.runtime.State<ProfileUiState> = _uiState

    val profile: StateFlow<HealthProfile> = profileRepository.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HealthProfile())

    val allergies: StateFlow<List<HealthAllergy>> = profileRepository.observeAllergies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<HealthContact>> = profileRepository.observeContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<MedicalDocument>> = profileRepository.observeDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveProfile(profile: HealthProfile) {
        viewModelScope.launch {
            profileRepository.saveProfile(profile)
            _uiState.value = _uiState.value.copy(successMessage = "saved")
        }
    }

    fun addAllergy(name: String, severity: String?, notes: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            profileRepository.addAllergy(
                HealthAllergy(name = name.trim(), severity = severity?.trim(), notes = notes?.trim())
            )
        }
    }

    fun deleteAllergy(allergy: HealthAllergy) {
        viewModelScope.launch { profileRepository.deleteAllergy(allergy) }
    }

    fun addContact(
        name: String,
        category: ContactCategory,
        specialty: String?,
        phone: String?,
        email: String?,
        address: String?,
        notes: String?,
        isEmergency: Boolean
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            profileRepository.addContact(
                HealthContact(
                    name = name.trim(),
                    category = category,
                    specialty = specialty?.trim(),
                    phone = phone?.trim(),
                    email = email?.trim(),
                    address = address?.trim(),
                    notes = notes?.trim(),
                    isEmergency = isEmergency
                )
            )
        }
    }

    fun deleteContact(contact: HealthContact) {
        viewModelScope.launch { profileRepository.deleteContact(contact) }
    }

    fun importDocument(
        uri: Uri,
        title: String,
        category: DocumentCategory,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                profileRepository.importDocument(
                    sourceUri = uri,
                    title = title,
                    category = category.name,
                    documentDateMillis = System.currentTimeMillis(),
                    notes = notes
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun deleteDocument(document: MedicalDocument) {
        viewModelScope.launch { profileRepository.deleteDocument(document) }
    }

    fun documentFile(document: MedicalDocument): File =
        profileRepository.documentFile(document)

    fun clearAll() {
        viewModelScope.launch { profileRepository.clearAll() }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }
}

data class ProfileUiState(
    val successMessage: String? = null,
    val errorMessage: String? = null
)
