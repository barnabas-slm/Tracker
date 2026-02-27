package com.example.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.tracker.data.TrackerDatabase
import com.example.tracker.ui.components.TrackerApp
import com.example.tracker.ui.theme.TrackerTheme
import com.example.tracker.viewmodel.CounterViewModel

class MainActivity : ComponentActivity() {
    private val counterViewModel: CounterViewModel by viewModels {
        CounterViewModel.Factory(TrackerDatabase.getInstance(applicationContext), applicationContext)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TrackerTheme { TrackerApp(viewModel = counterViewModel) } }
    }
}
