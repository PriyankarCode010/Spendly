package com.spendly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.spendly.app.core.ui.theme.SpendlyTheme
import com.spendly.app.navigation.SpendlyNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpendlyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SpendlyNavGraph()
                }
            }
        }
    }
}
