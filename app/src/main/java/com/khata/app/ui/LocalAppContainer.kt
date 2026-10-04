package com.khata.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.khata.app.AppContainer

/** Provided once in MainActivity so screens can build ViewModels without touching Context. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer was not provided. Wrap the UI in CompositionLocalProvider in MainActivity.")
}
