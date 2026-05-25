package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.IagiDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.IagiViewModel

class MainActivity : ComponentActivity() {
    private val iagiViewModel: IagiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Dynamic full edge-to-edge immersive presentation
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                IagiDashboardScreen(
                    viewModel = iagiViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
