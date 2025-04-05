package com.example.ethktprototype

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


/**
 * This class is a factory for creating instances of the WalletViewModel.
 *
 * @param application The application instance.
 */
class WalletViewModelFactory(private val application: Application) : ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WalletViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WalletViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}