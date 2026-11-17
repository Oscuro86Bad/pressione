package com.rudi.pressione

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rudi.pressione.data.Store
import com.rudi.pressione.ui.AppRoot
import com.rudi.pressione.ui.theme.TemaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        enableEdgeToEdge()
        setContent {
            TemaApp { AppRoot() }
        }
    }
}
