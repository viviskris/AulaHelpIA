package com.tuusuario.creciendojuntos.onboarding.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate

class OnboardingViewModel : ViewModel() {

    private val _onboardingComplete = MutableLiveData<Boolean>()
    val onboardingComplete: LiveData<Boolean> = _onboardingComplete

    private val _selectedDueDate = MutableLiveData<LocalDate?>()
    val selectedDueDate: LiveData<LocalDate?> = _selectedDueDate

    fun setDueDate(dueDate: LocalDate) {
        _selectedDueDate.value = dueDate
    }

    fun completeOnboarding() {
        _onboardingComplete.value = true
    }

    fun completeOnboardingWithDate(dueDate: LocalDate) {
        _selectedDueDate.value = dueDate
        _onboardingComplete.value = true
    }
}