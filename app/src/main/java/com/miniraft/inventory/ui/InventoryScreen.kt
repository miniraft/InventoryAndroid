package com.miniraft.inventory.ui

import android.graphics.BlurMaskFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniraft.inventory.data.Item
import com.miniraft.inventory.data.ItemStatus
import com.miniraft.inventory.ui.theme.StatusGreen
import com.miniraft.inventory.ui.theme.StatusRed
import com.miniraft.inventory.ui.theme.StatusYellow
import kotlinx.coroutines.delay

// Fixed memory allocation by caching the Paint object instead of drawing it behind on every frame
fun Modifier.glowShadow(
    color: Color = Color.Black.copy(alpha = 0.18f),
    borderRadius: Dp = 15.dp,
    blurRadius: Dp = 12.dp,
    spread: Dp = 2.dp
) = this.drawWithCache {
    val paint = Paint().apply {
        val frameworkPaint = asFrameworkPaint()
        frameworkPaint.color = color.toArgb()
        frameworkPaint.setMaskFilter(
            BlurMaskFilter(blurRadius.toPx(), BlurMaskFilter.Blur.NORMAL)
        )
    }
    onDrawBehind {
        drawIntoCanvas { canvas ->
            canvas.drawRoundRect(
                left = -spread.toPx(),
                top = -spread.toPx(),
                right = size.width + spread.toPx(),
                bottom = size.height + spread.toPx(),
                radiusX = borderRadius.toPx(),
                radiusY = borderRadius.toPx(),
                paint = paint
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onAddItemClick: () -> Unit,
    onEditItem: (Item) -> Unit
) {
    val filteredItems by viewModel.filteredItems.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val newlyAddedItemId by viewModel.newlyAddedItemId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var isSearchFocused by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val topPaddingAnim by animateDpAsState(
        targetValue = if (isSearchFocused) 44.dp else 0.dp,
        animationSpec = tween(durationMillis = 350),
        label = "topPadding"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AnimatedVisibility(
                visible = !isSearchFocused && searchQuery.isEmpty(),
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Inventory",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = "Sort Items"
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            viewModel.setSortOption(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = onAddItemClick) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Item"
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            SearchBarComponent(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onFocusChanged = { focused ->
                    isSearchFocused = focused
                },
                onClear = {
                    viewModel.updateSearchQuery("")
                    focusManager.clearFocus()
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
        ) {
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPaddingAnim),
                    contentAlignment = Alignment.Center
                ) {
                    if (searchQuery.isBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = "Click on the ",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Plus icon",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(horizontal = 2.dp)
                            )
                            Text(
                                text = " icon to add a new item",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Text(
                            text = "No matching items",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = topPaddingAnim)
                ) {
                    if (searchQuery.isNotBlank()) {
                        val sortedList = filteredItems.sortedBy { it.name.lowercase() }
                        items(sortedList, key = { it.id }) { item ->
                            SwipeableItemCard(
                                item = item,
                                isNewlyAdded = item.id == newlyAddedItemId,
                                onHighlightFinished = { viewModel.clearNewlyAddedItemId() },
                                onUpdateStatus = { i, status -> viewModel.updateItemStatus(i, status) },
                                onDeleteItem = { viewModel.deleteItem(it) },
                                onEditItem = onEditItem,
                                modifier = Modifier.animateItem()
                            )
                        }
                    } else {
                        when (sortOption) {
                            SortOption.NAME -> {
                                val sortedList = filteredItems.sortedBy { it.name.lowercase() }
                                items(sortedList, key = { it.id }) { item ->
                                    SwipeableItemCard(
                                        item = item,
                                        isNewlyAdded = item.id == newlyAddedItemId,
                                        onHighlightFinished = { viewModel.clearNewlyAddedItemId() },
                                        onUpdateStatus = { i, status -> viewModel.updateItemStatus(i, status) },
                                        onDeleteItem = { viewModel.deleteItem(it) },
                                        onEditItem = onEditItem,
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                            SortOption.STATUS -> {
                                val grouped = filteredItems
                                    .groupBy { it.status }
                                    .toSortedMap(compareBy { it.ordinal })

                                grouped.forEach { (status, statusItems) ->
                                    item(key = "header_status_${status.name}") {
                                        Text(
                                            text = status.label,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .animateItem()
                                                .padding(start = 20.dp, top = 28.dp, bottom = 8.dp)
                                        )
                                    }
                                    val sortedItems = statusItems.sortedBy { it.name.lowercase() }
                                    items(sortedItems, key = { it.id }) { item ->
                                        SwipeableItemCard(
                                            item = item,
                                            isNewlyAdded = item.id == newlyAddedItemId,
                                            onHighlightFinished = { viewModel.clearNewlyAddedItemId() },
                                            onUpdateStatus = { i, newStatus -> viewModel.updateItemStatus(i, newStatus) },
                                            onDeleteItem = { viewModel.deleteItem(it) },
                                            onEditItem = onEditItem,
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            }
                            SortOption.CATEGORY -> {
                                val grouped = filteredItems
                                    .groupBy { it.category }
                                    .toSortedMap(compareBy { it.lowercase() })

                                grouped.forEach { (category, categoryItems) ->
                                    item(key = "header_cat_$category") {
                                        Text(
                                            text = category,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .animateItem()
                                                .padding(start = 20.dp, top = 28.dp, bottom = 8.dp)
                                        )
                                    }
                                    val sortedItems = categoryItems.sortedBy { it.name.lowercase() }
                                    items(sortedItems, key = { it.id }) { item ->
                                        SwipeableItemCard(
                                            item = item,
                                            isNewlyAdded = item.id == newlyAddedItemId,
                                            onHighlightFinished = { viewModel.clearNewlyAddedItemId() },
                                            onUpdateStatus = { i, status -> viewModel.updateItemStatus(i, status) },
                                            onDeleteItem = { viewModel.deleteItem(it) },
                                            onEditItem = onEditItem,
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableItemCard(
    item: Item,
    isNewlyAdded: Boolean,
    onHighlightFinished: () -> Unit,
    onUpdateStatus: (Item, ItemStatus) -> Unit,
    onDeleteItem: (Item) -> Unit,
    onEditItem: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    var hasTriggeredEdit by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDeleteItem(item)
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!hasTriggeredEdit) {
                        hasTriggeredEdit = true
                        onEditItem(item)
                    }
                    false
                }
                else -> false
            }
        }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.Settled) {
            hasTriggeredEdit = false
        }
    }

    SwipeToDismissBox(
        modifier = modifier,
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFE53935)
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFFFF9800)
                else -> Color.Transparent
            }
            val icon = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                else -> Icons.Default.Delete
            }
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.Center
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(color, shape = RoundedCornerShape(15.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    ) {
        ItemCard(
            item = item,
            isNewlyAdded = isNewlyAdded,
            onHighlightFinished = onHighlightFinished,
            onUpdateStatus = onUpdateStatus,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ItemCard(
    item: Item,
    isNewlyAdded: Boolean,
    onHighlightFinished: () -> Unit,
    onUpdateStatus: (Item, ItemStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFading by remember { mutableStateOf(false) }

    LaunchedEffect(isNewlyAdded) {
        if (isNewlyAdded) {
            delay(5000L)
            isFading = true
            delay(1000L)
            onHighlightFinished()
        }
    }

    val targetStrokeWidth = if (isNewlyAdded && !isFading) 5.dp else 1.dp
    val targetStrokeColor = if (isNewlyAdded && !isFading) {
        when (item.status) {
            ItemStatus.IN_STOCK -> StatusGreen
            ItemStatus.LOW_STOCK -> StatusYellow
            ItemStatus.CRITICAL -> StatusRed
        }
    } else {
        MaterialTheme.colorScheme.outline
    }

    val animatedStrokeWidth by animateDpAsState(
        targetValue = targetStrokeWidth,
        animationSpec = tween(durationMillis = if (isFading) 1000 else 0),
        label = "strokeWidth"
    )

    val animatedStrokeColor by animateColorAsState(
        targetValue = targetStrokeColor,
        animationSpec = tween(durationMillis = if (isFading) 1000 else 0),
        label = "strokeColor"
    )

    val glowColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .glowShadow(color = glowColor, borderRadius = 15.dp, blurRadius = 12.dp, spread = 2.dp),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(animatedStrokeWidth, animatedStrokeColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy((-1).dp)
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.category,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusIconButton(
                    selected = item.status == ItemStatus.IN_STOCK,
                    activeColor = StatusGreen,
                    inactiveColor = MaterialTheme.colorScheme.outline,
                    onClick = { onUpdateStatus(item, ItemStatus.IN_STOCK) },
                    icon = Icons.Default.Check
                )
                StatusIconButton(
                    selected = item.status == ItemStatus.LOW_STOCK,
                    activeColor = StatusYellow,
                    inactiveColor = MaterialTheme.colorScheme.outline,
                    onClick = { onUpdateStatus(item, ItemStatus.LOW_STOCK) },
                    textLabel = "!"
                )
                StatusIconButton(
                    selected = item.status == ItemStatus.CRITICAL,
                    activeColor = StatusRed,
                    inactiveColor = MaterialTheme.colorScheme.outline,
                    onClick = { onUpdateStatus(item, ItemStatus.CRITICAL) },
                    icon = Icons.Default.Close
                )
            }
        }
    }
}

@Composable
fun StatusIconButton(
    selected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    textLabel: String? = null
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) activeColor else Color.Transparent)
            .border(
                width = 2.5.dp,
                color = if (selected) activeColor else inactiveColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else inactiveColor,
                modifier = Modifier.size(14.dp)
            )
        } else if (textLabel != null) {
            Text(
                text = textLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (selected) Color.White else inactiveColor
            )
        }
    }
}

@Composable
fun SearchBarComponent(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 12.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 16.dp)
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                }
        )
    }
}
