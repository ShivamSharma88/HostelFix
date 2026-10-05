package com.example.hostelfix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.hostelfix.ui.home.HomeScreen
import com.example.hostelfix.ui.theme.HostelFixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HostelFixTheme {
                HomeScreen()
            }
        }
    }
}