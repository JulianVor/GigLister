package com.giglister.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** No DI framework in this app - every screen's ViewModel just needs one or two plain
 * constructor arguments (a repository, mostly), so a generic factory wrapping a lambda is
 * all `viewModel(factory = ...)` needs. */
class SimpleViewModelFactory<T : ViewModel>(private val create: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}
