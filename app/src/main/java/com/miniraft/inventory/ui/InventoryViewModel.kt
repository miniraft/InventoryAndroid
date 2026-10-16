package com.miniraft.inventory.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniraft.inventory.data.Category
import com.miniraft.inventory.data.CategoryDatabase
import com.miniraft.inventory.data.Item
import com.miniraft.inventory.data.ItemDatabase
import com.miniraft.inventory.data.ItemStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SortOption(val label: String) {
    NAME("Name"),
    STATUS("Status"),
    CATEGORY("Category")
}

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val itemDao = ItemDatabase.getDatabase(application).itemDao()
    private val categoryDao = CategoryDatabase.getDatabase(application).categoryDao()
    private val prefs = application.getSharedPreferences("inventory_prefs", Context.MODE_PRIVATE)

    // Changed initialValue to null to represent a "loading" state
    val items: StateFlow<List<Item>?> = itemDao.getAllItems().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val categories: StateFlow<List<Category>> = categoryDao.getAllCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Now handles nullable list to delay UI rendering until DB is loaded
    val filteredItems: StateFlow<List<Item>?> = combine(items, _searchQuery) { list, query ->
        if (list == null) return@combine null

        if (query.isBlank()) {
            list
        } else {
            val q = query.trim().lowercase()
            list.filter { item ->
                item.name.lowercase().contains(q) ||
                        item.category.lowercase().contains(q) ||
                        item.status.label.lowercase().contains(q)
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _sortOption = MutableStateFlow(
        try {
            val saved = prefs.getString("sort_option", SortOption.NAME.name) ?: SortOption.NAME.name
            SortOption.valueOf(saved)
        } catch (_: Exception) {
            SortOption.NAME
        }
    )
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
        prefs.edit().putString("sort_option", option.name).apply()
    }

    private val _newlyAddedItemId = MutableStateFlow<Int?>(null)
    val newlyAddedItemId: StateFlow<Int?> = _newlyAddedItemId.asStateFlow()

    fun addItem(name: String, categoryName: String, status: ItemStatus, onComplete: () -> Unit) {
        if (name.isBlank()) return
        val categoryToSave = categoryName.trim().ifBlank { "Uncategorized" }
        viewModelScope.launch {
            if (categoryToSave.isNotBlank() && !categoryToSave.equals("Uncategorized", ignoreCase = true)) {
                val existing = categories.value.any { it.name.equals(categoryToSave, ignoreCase = true) }
                if (!existing) {
                    val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    categoryDao.insertCategory(Category(name = categoryToSave, creationDate = currentDate))
                }
            }
            val insertedId = itemDao.insertItem(Item(name = name.trim(), category = categoryToSave, status = status))
            _newlyAddedItemId.value = insertedId.toInt()
            onComplete()
        }
    }

    fun clearNewlyAddedItemId() {
        _newlyAddedItemId.value = null
    }

    fun updateItemStatus(item: Item, newStatus: ItemStatus) {
        viewModelScope.launch { itemDao.updateItem(item.copy(status = newStatus)) }
    }

    fun updateItemDetails(item: Item, name: String, categoryName: String, onComplete: () -> Unit) {
        if (name.isBlank()) return
        val categoryToSave = categoryName.trim().ifBlank { "Uncategorized" }
        viewModelScope.launch {
            if (categoryToSave.isNotBlank() && !categoryToSave.equals("Uncategorized", ignoreCase = true)) {
                val existing = categories.value.any { it.name.equals(categoryToSave, ignoreCase = true) }
                if (!existing) {
                    val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    categoryDao.insertCategory(Category(name = categoryToSave, creationDate = currentDate))
                }
            }
            itemDao.updateItem(item.copy(name = name.trim(), category = categoryToSave))
            onComplete()
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch { itemDao.deleteItem(item) }
    }

    fun addCategory(categoryName: String, onAdded: (() -> Unit)? = null) {
        if (categoryName.isBlank()) return
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            categoryDao.insertCategory(Category(name = categoryName.trim(), creationDate = currentDate))
            onAdded?.invoke()
        }
    }

    fun canDeleteCategory(categoryName: String): Boolean {
        // Now safely checks against nullable list
        return items.value?.none { it.category.equals(categoryName, ignoreCase = true) } ?: false
    }

    fun deleteCategory(category: Category, onError: ((String) -> Unit)? = null) {
        if (!canDeleteCategory(category.name)) {
            onError?.invoke("Cannot delete category because it is currently in use by one or more items.")
            return
        }
        viewModelScope.launch { categoryDao.deleteCategory(category) }
    }
}