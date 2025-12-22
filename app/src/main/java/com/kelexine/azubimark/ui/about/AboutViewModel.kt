package com.kelexine.azubimark.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelexine.azubimark.data.model.GithubUser
import com.kelexine.azubimark.data.remote.GithubService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class AboutViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AboutUiState>(AboutUiState.Loading)
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    private val service: GithubService

    init {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        service = retrofit.create(GithubService::class.java)

        fetchDeveloperInfo()
    }

    private fun fetchDeveloperInfo() {
        viewModelScope.launch {
            try {
                _uiState.value = AboutUiState.Loading
                val user = service.getUser("kelexine")
                _uiState.value = AboutUiState.Success(user)
            } catch (e: Exception) {
                _uiState.value = AboutUiState.Error(e.message ?: "Failed to load developer info")
            }
        }
    }
}

sealed class AboutUiState {
    object Loading : AboutUiState()
    data class Success(val user: GithubUser) : AboutUiState()
    data class Error(val message: String) : AboutUiState()
}
