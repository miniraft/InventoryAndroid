package com.miniraft.inventory.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniraft.inventory.data.ItemStatus
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    viewModel: InventoryViewModel,
    onBackClick: () -> Unit,
    onSaveClick: (name: String, category: String, status: ItemStatus) -> Unit
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<ItemStatus?>(null) }
    var selectedCategory by remember { mutableStateOf("") }

    var showStatusDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = {
                    Text("Add Item", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (name.isBlank()) errorMessage = "The name of the category cannot be empty."
                            else if (selectedStatus == null) errorMessage = "Please choose a status for the current item."
                            else if (selectedCategory.isBlank()) errorMessage = "Please choose a category for the current item."
                            else onSaveClick(name, selectedCategory, selectedStatus!!)
                        }
                    ) { Icon(Icons.Default.Check, contentDescription = "Save Item") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Name", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth(0.55f).height(54.dp),
                    decorationBox = { innerTextField ->
                        OutlinedTextFieldDefaults.DecorationBox(
                            value = name,
                            innerTextField = innerTextField,
                            enabled = true,
                            singleLine = true,
                            visualTransformation = VisualTransformation.None,
                            interactionSource = remember { MutableInteractionSource() },
                            placeholder = { Text("Enter name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            container = {
                                OutlinedTextFieldDefaults.Container(
                                    enabled = true,
                                    isError = false,
                                    interactionSource = remember { MutableInteractionSource() },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    shape = CircleShape
                                )
                            }
                        )
                    }
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Status", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                OutlinedButton(onClick = { focusManager.clearFocus(); showStatusDialog = true }, shape = CircleShape, modifier = Modifier.height(48.dp)) {
                    Text(selectedStatus?.label ?: "Choose...")
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Category", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                OutlinedButton(onClick = { focusManager.clearFocus(); showCategoryDialog = true }, shape = CircleShape, modifier = Modifier.height(48.dp)) {
                    Text(selectedCategory.ifBlank { "Choose..." })
                }
            }
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null }, title = { Text("Error") }, text = { Text(errorMessage!!) },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK") } }
        )
    }

    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false }, title = { Text("Select Status") },
            text = {
                Surface(
                    shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                ) {
                    Column {
                        ItemStatus.entries.forEachIndexed { index, status ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            TextButton(onClick = { selectedStatus = status; showStatusDialog = false }, modifier = Modifier.fillMaxWidth()) {
                                Text(status.label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showStatusDialog = false }) { Text("Cancel") } }
        )
    }

    if (showCategoryDialog) {
        var newCategoryText by remember { mutableStateOf("") }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val coroutineScope = rememberCoroutineScope()

        ModalBottomSheet(onDismissRequest = { showCategoryDialog = false }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
            val bottomSheetFocusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current
            val sheetFocusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                sheetFocusRequester.requestFocus()
                keyboardController?.hide()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .focusRequester(sheetFocusRequester)
                    .focusable()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { bottomSheetFocusManager.clearFocus() })
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Select Category", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BasicTextField(
                        value = newCategoryText,
                        onValueChange = { newCategoryText = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f).height(52.dp),
                        decorationBox = { innerTextField ->
                            OutlinedTextFieldDefaults.DecorationBox(
                                value = newCategoryText,
                                innerTextField = innerTextField,
                                enabled = true,
                                singleLine = true,
                                visualTransformation = VisualTransformation.None,
                                interactionSource = remember { MutableInteractionSource() },
                                placeholder = { Text("New category") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                container = {
                                    OutlinedTextFieldDefaults.Container(
                                        enabled = true,
                                        isError = false,
                                        interactionSource = remember { MutableInteractionSource() },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        ),
                                        shape = CircleShape
                                    )
                                }
                            )
                        }
                    )
                    Button(
                        onClick = {
                            if (newCategoryText.isNotBlank()) {
                                val catName = newCategoryText.trim()
                                viewModel.addCategory(catName)
                                selectedCategory = catName
                                newCategoryText = ""
                                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { showCategoryDialog = false }
                            }
                        },
                        shape = CircleShape, modifier = Modifier.height(52.dp)
                    ) { Text("Add", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp)) }
                }

                if (categories.isEmpty()) {
                    Text("No categories available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it.id }) { category ->
                            CategorySwipeItem(
                                categoryName = category.name,
                                onDelete = {
                                    if (viewModel.canDeleteCategory(category.name)) {
                                        viewModel.deleteCategory(category) { err -> errorMessage = err }
                                        if (selectedCategory.equals(category.name, ignoreCase = true)) selectedCategory = ""
                                        true
                                    } else {
                                        errorMessage = "Cannot delete category because it is currently in use by one or more items."
                                        false
                                    }
                                },
                                onSelect = {
                                    selectedCategory = category.name
                                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { showCategoryDialog = false }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySwipeItem(categoryName: String, onDelete: () -> Boolean, onSelect: () -> Unit) {
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var hasTriggered by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val threshold = with(density) { 80.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetX.value < -threshold && !hasTriggered) {
                                hasTriggered = true
                                if (onDelete()) {
                                    offsetX.animateTo(-size.width.toFloat(), tween(200))
                                } else {
                                    offsetX.animateTo(0f, tween(200))
                                    hasTriggered = false
                                }
                            } else {
                                offsetX.animateTo(0f, tween(200))
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (!hasTriggered && dragAmount < 0) {
                            coroutineScope.launch {
                                offsetX.snapTo((offsetX.value + dragAmount).coerceAtMost(0f))
                            }
                        }
                    }
                )
            }
    ) {
        val offsetVal = offsetX.value
        val color = if (offsetVal < -10f) Color(0xFFE53935) else Color.Transparent

        Box(
            modifier = Modifier.matchParentSize().background(color, shape = RoundedCornerShape(10.dp)).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (offsetVal < -10f) {
                val scale = (abs(offsetVal) / threshold).coerceIn(0.5f, 1f)
                Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = Color.White, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
            }
        }
        Surface(
            shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().graphicsLayer { translationX = offsetX.value }
        ) {
            TextButton(onClick = onSelect, modifier = Modifier.fillMaxWidth()) {
                Text(categoryName, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}