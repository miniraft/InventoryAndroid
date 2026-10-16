package com.miniraft.inventory.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

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
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    val exitSearch = {
        keyboardController?.hide()
        focusManager.clearFocus()
        viewModel.updateSearchQuery("")
        isSearchFocused = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        val isSearchActiveMode = isSearchFocused || searchQuery.isNotEmpty()
        val topBarHeightPx = with(LocalDensity.current) { 64.dp.toPx() }

        val verticalOffset by animateFloatAsState(
            targetValue = if (isSearchActiveMode) -topBarHeightPx else 0f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            label = "verticalOffset"
        )

        val headerAlpha by animateFloatAsState(
            targetValue = if (isSearchActiveMode) 0f else 1f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            label = "headerAlpha"
        )

        LaunchedEffect(isSearchActiveMode) {
            if (!isSearchActiveMode) {
                listState.animateScrollToItem(0)
            }
        }

        LaunchedEffect(sortOption) {
            listState.animateScrollToItem(0)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isSearchActiveMode) {
                    detectTapGestures(onTap = {
                        if (isSearchActiveMode) {
                            exitSearch()
                        } else {
                            focusManager.clearFocus()
                        }
                    })
                }
        ) {
            if (filteredItems == null) {
                Box(modifier = Modifier.fillMaxSize())
            } else if (filteredItems!!.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = verticalOffset },
                    contentAlignment = Alignment.Center
                ) {
                    if (searchQuery.isBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(text = "Click the ", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Plus icon",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(20.dp).padding(horizontal = 2.dp)
                            )
                            Text(text = " icon to add a new item", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                        }
                    } else {
                        Text(text = "No matching items", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = verticalOffset },
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 64.dp,
                        bottom = innerPadding.calculateBottomPadding() + 96.dp
                    )
                ) {
                    val currentItems = filteredItems!!

                    if (isSearchActiveMode) {
                        item(key = "search_spacer") { Spacer(modifier = Modifier.height(28.dp)) }
                        val sortedList = currentItems.sortedBy { it.name.lowercase() }
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
                                item(key = "name_spacer") { Spacer(modifier = Modifier.height(28.dp)) }
                                val sortedList = currentItems.sortedBy { it.name.lowercase() }
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
                                val grouped = currentItems.groupBy { it.status }.toSortedMap(compareBy { it.ordinal })
                                grouped.forEach { (status, statusItems) ->
                                    item(key = "header_status_${status.name}") {
                                        Text(
                                            text = status.label,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.animateItem().padding(start = 20.dp, top = 28.dp, bottom = 8.dp)
                                        )
                                    }
                                    items(statusItems.sortedBy { it.name.lowercase() }, key = { it.id }) { item ->
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
                                val grouped = currentItems.groupBy { it.category }.toSortedMap(compareBy { it.lowercase() })
                                grouped.forEach { (category, categoryItems) ->
                                    item(key = "header_cat_$category") {
                                        Text(
                                            text = category,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.animateItem().padding(start = 20.dp, top = 28.dp, bottom = 8.dp)
                                        )
                                    }
                                    items(categoryItems.sortedBy { it.name.lowercase() }, key = { it.id }) { item ->
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = verticalOffset
                        alpha = headerAlpha
                    }
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(bottom = 32.dp)
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
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = "Sort Items")
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = { viewModel.setSortOption(option); showSortMenu = false }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onAddItemClick) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.70f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .padding(top = 28.dp)
            ) {
                SearchBarComponent(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    isSearchActive = isSearchFocused,
                    onFocusChanged = { focused -> isSearchFocused = focused },
                    onClear = {
                        viewModel.updateSearchQuery("")
                        focusManager.clearFocus()
                    },
                    onCloseSearch = {
                        viewModel.updateSearchQuery("")
                        isSearchFocused = false
                    }
                )
            }
        }
    }
}

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
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var hasTriggered by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val threshold = with(density) { 100.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetX.value < -threshold && !hasTriggered) {
                                hasTriggered = true
                                offsetX.animateTo(-size.width.toFloat(), tween(250, easing = FastOutSlowInEasing))
                                onDeleteItem(item)
                            } else if (offsetX.value > threshold && !hasTriggered) {
                                hasTriggered = true
                                offsetX.animateTo(size.width.toFloat(), tween(250, easing = FastOutSlowInEasing))
                                onEditItem(item)
                                offsetX.animateTo(0f, tween(250))
                                hasTriggered = false
                            } else {
                                offsetX.animateTo(0f, tween(250, easing = FastOutSlowInEasing))
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (!hasTriggered) {
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount)
                            }
                        }
                    }
                )
            }
    ) {
        val offsetVal = offsetX.value
        val color = when {
            offsetVal < -10f -> Color(0xFFE53935)
            offsetVal > 10f -> Color(0xFFFF9800)
            else -> Color.Transparent
        }
        val alignment = when {
            offsetVal < -10f -> Alignment.CenterEnd
            offsetVal > 10f -> Alignment.CenterStart
            else -> Alignment.Center
        }
        val icon = when {
            offsetVal < -10f -> Icons.Default.Delete
            offsetVal > 10f -> Icons.Default.Edit
            else -> Icons.Default.Delete
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(color, shape = RoundedCornerShape(18.dp))
                .padding(horizontal = 20.dp),
            contentAlignment = alignment
        ) {
            if (offsetVal < -10f || offsetVal > 10f) {
                val scale = (abs(offsetVal) / threshold).coerceIn(0.5f, 1f)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                )
            }
        }

        Box(modifier = Modifier.graphicsLayer { translationX = offsetVal }) {
            ItemCard(
                item = item,
                isNewlyAdded = isNewlyAdded,
                onHighlightFinished = onHighlightFinished,
                onUpdateStatus = onUpdateStatus,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
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
            delay(3.seconds)
            isFading = true
            delay(0.5.seconds)
            onHighlightFinished()
        }
    }

    val targetStrokeColor = if (isNewlyAdded && !isFading) {
        when (item.status) {
            ItemStatus.IN_STOCK -> StatusGreen
            ItemStatus.LOW_STOCK -> StatusYellow
            ItemStatus.CRITICAL -> StatusRed
        }
    } else {
        MaterialTheme.colorScheme.outline
    }

    val animatedStrokeColor by animateColorAsState(
        targetValue = targetStrokeColor,
        animationSpec = tween(durationMillis = if (isFading) 1000 else 0),
        label = "strokeColor"
    )

    val targetStrokeWidth = if (isNewlyAdded && !isFading) 2.dp else 1.dp

    val animatedStrokeWidth by animateDpAsState(
        targetValue = targetStrokeWidth,
        animationSpec = tween(durationMillis = if (isFading) 1000 else 0),
        label = "strokeWidth"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .drawCustomShadow(
                color = Color.Black.copy(alpha = 0.20f),
                borderRadius = 18.dp,
                blurRadius = 10.dp,
                offsetY = 3.dp
            ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(animatedStrokeWidth, animatedStrokeColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 9.dp),
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
    val tintColor = if (selected) Color.White else inactiveColor

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
        if (icon == Icons.Default.Check) {
            Canvas(modifier = Modifier.size(11.dp)) {
                val strokeWidth = 2.5.dp.toPx()
                val path = Path().apply {
                    moveTo(size.width * 0.10f, size.height * 0.52f)
                    lineTo(size.width * 0.40f, size.height * 0.82f)
                    lineTo(size.width * 0.90f, size.height * 0.22f)
                }
                drawPath(path = path, color = tintColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        } else if (icon == Icons.Default.Close) {
            Canvas(modifier = Modifier.size(10.dp)) {
                val strokeWidth = 2.5.dp.toPx()
                drawLine(color = tintColor, start = Offset(0f, 0f), end = Offset(size.width, size.height), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tintColor, start = Offset(size.width, 0f), end = Offset(0f, size.height), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
        } else if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(14.dp))
        } else if (textLabel != null) {
            Text(text = textLabel, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = tintColor)
        }
    }
}

@Composable
fun SearchBarComponent(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearchActive: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onClear: () -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = isSearchActive) {
        keyboardController?.hide()
        onFocusChanged(false)
        onCloseSearch()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .drawCustomShadow(
                color = Color.Black.copy(alpha = 0.10f),
                borderRadius = 27.dp,
                blurRadius = 10.dp,
                offsetY = 3.dp
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (isSearchActive) {
                        keyboardController?.hide()
                        onFocusChanged(false)
                        onCloseSearch()
                    } else {
                        onFocusChanged(true)
                    }
                }
            ) {
                Icon(
                    imageVector = if (isSearchActive) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Search,
                    contentDescription = if (isSearchActive) "Exit Search" else "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (!isSearchActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onFocusChanged(true) }
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Search",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        })
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                }
            }

            if (query.isNotEmpty() && isSearchActive) {
                IconButton(onClick = { onClear(); onQueryChange("") }) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Box(modifier = Modifier.padding(24.dp))
            }
        }
    }
}

fun Modifier.drawCustomShadow(
    color: Color = Color.Black.copy(alpha = 0.10f),
    borderRadius: Dp = 18.dp,
    blurRadius: Dp = 10.dp,
    offsetY: Dp = 3.dp,
    spread: Dp = 0.dp
): Modifier = this.drawBehind {
    val shadowColor = color.toArgb()
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = shadowColor
        if (blurRadius > 0.dp) {
            maskFilter = android.graphics.BlurMaskFilter(
                blurRadius.toPx().coerceAtLeast(1f),
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }
    }

    drawIntoCanvas { canvas ->
        val left = -spread.toPx()
        val top = offsetY.toPx() - spread.toPx()
        val right = size.width + spread.toPx()
        val bottom = size.height + offsetY.toPx() + spread.toPx()
        val rx = borderRadius.toPx()

        canvas.nativeCanvas.drawRoundRect(
            left, top, right, bottom, rx, rx, paint
        )
    }
}