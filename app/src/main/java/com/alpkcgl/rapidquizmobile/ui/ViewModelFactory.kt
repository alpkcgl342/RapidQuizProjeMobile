package com.alpkcgl.rapidquizmobile.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.alpkcgl.rapidquizmobile.AppContainer
import com.alpkcgl.rapidquizmobile.RapidQuizApp

/** `viewModelFactory { initializer { … } }` içinde AppContainer'a erişim. */
fun CreationExtras.appContainer(): AppContainer = (this[APPLICATION_KEY] as RapidQuizApp).container
