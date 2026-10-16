package com.miniraft.inventory.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNavigation(viewModel: InventoryViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "inventory",
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(350))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(350))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(350))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(350))
        }
    ) {
        composable("inventory") {
            InventoryScreen(
                viewModel = viewModel,
                onAddItemClick = { navController.navigate("add_item") },
                onEditItem = { item -> navController.navigate("edit_item/${item.id}") }
            )
        }
        composable("add_item") {
            AddItemScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onSaveClick = { name, category, status ->
                    viewModel.addItem(name, category, status) {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable(
            route = "edit_item/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.IntType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
            val items by viewModel.items.collectAsStateWithLifecycle()
            val item = items?.find { it.id == itemId }

            if (item != null) {
                EditItemScreen(
                    item = item,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = { name, category ->
                        viewModel.updateItemDetails(item, name, category) {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}