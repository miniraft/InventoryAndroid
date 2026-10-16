package com.miniraft.inventory.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class ItemStatus(val label: String) {
    CRITICAL("Critical"),
    LOW_STOCK("Low Stock"),
    IN_STOCK("In Stock")
}

object ItemStatusConverter {
    @TypeConverter
    fun fromStatus(status: ItemStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ItemStatus = try {
        ItemStatus.valueOf(value)
    } catch (_: Exception) {
        ItemStatus.IN_STOCK
    }
}

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val category: String,
    val status: ItemStatus
)
