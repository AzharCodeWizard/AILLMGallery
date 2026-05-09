package com.azhar.aillmgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.azhar.aillmgallery.navigation.AppNavigation
import com.azhar.aillmgallery.ui.theme.AILLMGalleryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AILLMGalleryTheme {
                AppNavigation()
            }
        }
    }
}