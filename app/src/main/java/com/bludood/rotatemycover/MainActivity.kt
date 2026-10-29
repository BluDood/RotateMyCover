package com.bludood.rotatemycover

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bludood.rotatemycover.ui.MainContainer
import com.bludood.rotatemycover.ui.theme.RotateMyCoverTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RotationManager.init(this)
        enableEdgeToEdge()
        setContent {
            RotateMyCoverTheme {
                MainContainer()
            }
        }
    }
}
