package com.example.ethktprototype

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner


/**
 * Application class that implements [ViewModelStoreOwner] to provide an application-scoped ViewModel.
 */
class HealthyWalletApplication(override val viewModelStore: ViewModelStore) : Application(), ViewModelStoreOwner {
    private val appViewModelStore: ViewModelStore by lazy { ViewModelStore() }

    /**
     * Public constructor without arguments needed for application initialization.
     */
    constructor() : this(ViewModelStore())
}