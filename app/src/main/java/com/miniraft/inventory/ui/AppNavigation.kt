package com.miniraft.inventory.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object Inventory : Screen("inventory")
    object AddItem : Screen("add_item")
    object EditItem : Screen("edit_item/{itemId}") {
        fun createRoute(itemId: Int) = "edit_item/$itemId"
    }
}

@Composable
fun AppNavigation(viewModel: InventoryViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Inventory.route,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350)
            )
        }
    ) {
        composable(Screen.Inventory.route) {
            InventoryScreen(
                viewModel = viewModel,
                onAddItemClick = { navController.navigate(Screen.AddItem.route) },
                onEditItem = { item ->
                    navController.navigate(Screen.EditItem.createRoute(item.id))
                }
            )
        }

        composable(Screen.AddItem.route) {
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
            route = Screen.EditItem.route,
            arguments = listOf(navArgument("itemId") { type = NavType.IntType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
            val targetItem = viewModel.items.value.find { it.id == itemId } ?: return@composable

            EditItemScreen(
                item = targetItem,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onSaveClick = { name, category ->
                    viewModel.updateItemDetails(targetItem, name, category) {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}
