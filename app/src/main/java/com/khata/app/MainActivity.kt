package com.khata.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.khata.app.ui.KhataApp
import com.khata.app.ui.LocalAppContainer
import com.khata.app.ui.theme.KhataTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as KhataApplication).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                KhataTheme {
                    KhataApp()
                }
            }
        }
    }
}
