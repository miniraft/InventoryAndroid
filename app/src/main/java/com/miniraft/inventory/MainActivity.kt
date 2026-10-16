package com.miniraft.inventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.miniraft.inventory.ui.AppNavigation
import com.miniraft.inventory.ui.InventoryViewModel
import com.miniraft.inventory.ui.theme.InventoryTheme

class MainActivity : ComponentActivity() {
    private val viewModel: InventoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InventoryTheme {
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}
