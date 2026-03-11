package com.example.lspandroid.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Plugin information for browser display with comprehensive metadata.
 */
data class PluginInfo(
    val pluginId: String,
    val pluginName: String,
    val category: String,
    val description: String,
    val parameterCount: Int,
    val version: String = "1.0.0",
    val author: String = "Unknown",
    val rating: Float = 0f,
    val downloadCount: Int = 0,
    val lastUpdated: Date = Date(),
    val tags: List<String> = emptyList(),
    val isVerified: Boolean = false,
    val isPremium: Boolean = false,
    val fileSize: String = "0 KB",
    val compatibility: List<String> = emptyList(),
    val documentation: String = "",
    val changelog: String = "",
    val dependencies: List<String> = emptyList(),
    val permissions: List<String> = emptyList()
)

/**
 * Sort options for plugin list.
 */
enum class PluginSortOption(val displayName: String) {
    NAME("Name"),
    RATING("Rating"),
    DOWNLOADS("Downloads"),
    UPDATED("Last Updated"),
    SIZE("File Size")
}
/**
 * View mode for plugin display.
 */
enum class PluginViewMode {
    LIST,
    GRID,
    COMPACT
}
/**
 * Plugin Browser UI component with advanced filtering, sorting, and search capabilities.
 * Displays categorized plugin list with comprehensive metadata and user interactions.
 * 
 * Requirement 12: Plugin Browser - Enhanced Implementation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginBrowser(
    plugins: List<PluginInfo>,
    onPluginSelected: (PluginInfo) -> Unit,
    onPluginFavorited: (PluginInfo, Boolean) -> Unit = { _, _ -> },
    onPluginInfo: (PluginInfo) -> Unit = { },
    favoritePlugins: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sortOption by remember { mutableStateOf(PluginSortOption.NAME) }
    var viewMode by remember { mutableStateOf(PluginViewMode.LIST) }
    var showFilters by remember { mutableStateOf(false) }
    var minRating by remember { mutableStateOf(0f) }
    var showVerifiedOnly by remember { mutableStateOf(false) }
    var showFreeOnly by remember { mutableStateOf(false) }
    var expandedPlugin by remember { mutableStateOf<String?>(null) }

    val categories = plugins.map { it.category }.distinct().sorted()
    val allTags = plugins.flatMap { it.tags }.distinct().sorted()

    val filteredAndSortedPlugins = remember(
        plugins, searchQuery, selectedCategory, selectedTags, sortOption, 
        minRating, showVerifiedOnly, showFreeOnly
    ) {
        plugins.filter { plugin ->
            val matchesSearch = searchQuery.isEmpty() || 
                plugin.pluginName.contains(searchQuery, ignoreCase = true) ||
                plugin.description.contains(searchQuery, ignoreCase = true) ||
                plugin.author.contains(searchQuery, ignoreCase = true) ||
                plugin.tags.any { it.contains(searchQuery, ignoreCase = true) }
            
            val matchesCategory = selectedCategory == null || plugin.category == selectedCategory
            val matchesTags = selectedTags.isEmpty() || selectedTags.any { it in plugin.tags }
            val matchesRating = plugin.rating >= minRating
            val matchesVerified = !showVerifiedOnly || plugin.isVerified
            val matchesFree = !showFreeOnly || !plugin.isPremium
            
            matchesSearch && matchesCategory && matchesTags && matchesRating && matchesVerified && matchesFree
        }.sortedWith { a, b ->
            when (sortOption) {
                PluginSortOption.NAME -> a.pluginName.compareTo(b.pluginName, ignoreCase = true)
                PluginSortOption.RATING -> b.rating.compareTo(a.rating)
                PluginSortOption.DOWNLOADS -> b.downloadCount.compareTo(a.downloadCount)
                PluginSortOption.UPDATED -> b.lastUpdated.compareTo(a.lastUpdated)
                PluginSortOption.SIZE -> a.fileSize.compareTo(b.fileSize)
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with search and controls
        PluginBrowserHeader(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            viewMode = viewMode,
            onViewModeChange = { viewMode = it },
            showFilters = showFilters,
            onToggleFilters = { showFilters = !showFilters },
            sortOption = sortOption,
            onSortOptionChange = { sortOption = it },
            resultCount = filteredAndSortedPlugins.size,
            totalCount = plugins.size
        )

        // Advanced filters panel
        AnimatedVisibility(
            visible = showFilters,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            AdvancedFiltersPanel(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                allTags = allTags,
                selectedTags = selectedTags,
                onTagsChanged = { selectedTags = it },
                minRating = minRating,
                onMinRatingChange = { minRating = it },
                showVerifiedOnly = showVerifiedOnly,
                onShowVerifiedOnlyChange = { showVerifiedOnly = it },
                showFreeOnly = showFreeOnly,
                onShowFreeOnlyChange = { showFreeOnly = it }
            )
        }

        // Plugin list/grid
        when (viewMode) {
            PluginViewMode.LIST -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAndSortedPlugins, key = { it.pluginId }) { plugin ->
                        PluginListItem(
                            plugin = plugin,
                            onSelected = { onPluginSelected(plugin) },
                            onFavorited = { onPluginFavorited(plugin, it) },
                            onInfo = { onPluginInfo(plugin) },
                            isFavorited = plugin.pluginId in favoritePlugins,
                            isExpanded = expandedPlugin == plugin.pluginId,
                            onToggleExpanded = { 
                                expandedPlugin = if (expandedPlugin == plugin.pluginId) null else plugin.pluginId 
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            PluginViewMode.GRID -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAndSortedPlugins.chunked(2)) { rowPlugins ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPlugins.forEach { plugin ->
                                PluginGridItem(
                                    plugin = plugin,
                                    onSelected = { onPluginSelected(plugin) },
                                    onFavorited = { onPluginFavorited(plugin, it) },
                                    isFavorited = plugin.pluginId in favoritePlugins,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowPlugins.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            PluginViewMode.COMPACT -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredAndSortedPlugins, key = { it.pluginId }) { plugin ->
                        PluginCompactItem(
                            plugin = plugin,
                            onSelected = { onPluginSelected(plugin) },
                            onFavorited = { onPluginFavorited(plugin, it) },
                            isFavorited = plugin.pluginId in favoritePlugins,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
/**
 * Plugin browser header with search, view controls, and statistics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginBrowserHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    viewMode: PluginViewMode,
    onViewModeChange: (PluginViewMode) -> Unit,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    sortOption: PluginSortOption,
    onSortOptionChange: (PluginSortOption) -> Unit,
    resultCount: Int,
    totalCount: Int
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Search bar with advanced features
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = { Text("Search plugins, authors, tags...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            ),
            singleLine = true
        )

        // Controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Results info
            Text(
                text = "$resultCount of $totalCount plugins",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Sort dropdown
                var showSortMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Sort: ${sortOption.displayName}",
                            fontSize = 12.sp
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        PluginSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    onSortOptionChange(option)
                                    showSortMenu = false
                                },
                                leadingIcon = if (option == sortOption) {
                                    { Icon(Icons.Default.Star, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }

                // View mode toggle
                Row(
                    modifier = Modifier
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(2.dp)
                ) {
                    PluginViewMode.values().forEach { mode ->
                        val isSelected = viewMode == mode
                        val backgroundColor by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            label = "background"
                        )
                        val contentColor by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            label = "content"
                        )

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(backgroundColor, RoundedCornerShape(2.dp))
                                .clickable { onViewModeChange(mode) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (mode) {
                                    PluginViewMode.LIST -> "☰"
                                    PluginViewMode.GRID -> "⊞"
                                    PluginViewMode.COMPACT -> "≡"
                                },
                                color = contentColor,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Filters toggle
                FilterChip(
                    onClick = onToggleFilters,
                    label = { Text("Filters", fontSize = 12.sp) },
                    selected = showFilters,
                    modifier = Modifier.height(32.dp)
                )
            }
        }
    }
}

/**
 * Advanced filters panel with comprehensive filtering options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedFiltersPanel(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    allTags: List<String>,
    selectedTags: Set<String>,
    onTagsChanged: (Set<String>) -> Unit,
    minRating: Float,
    onMinRatingChange: (Float) -> Unit,
    showVerifiedOnly: Boolean,
    onShowVerifiedOnlyChange: (Boolean) -> Unit,
    showFreeOnly: Boolean,
    onShowFreeOnlyChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Advanced Filters",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            // Category filter
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        FilterChip(
                            onClick = { onCategorySelected(null) },
                            label = { Text("All", fontSize = 11.sp) },
                            selected = selectedCategory == null
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            onClick = { onCategorySelected(category) },
                            label = { Text(category, fontSize = 11.sp) },
                            selected = selectedCategory == category
                        )
                    }
                }
            }

            // Tags filter
            if (allTags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tags",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedTags.isNotEmpty()) {
                            TextButton(
                                onClick = { onTagsChanged(emptySet()) },
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Clear", fontSize = 10.sp)
                            }
                        }
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(allTags.take(10)) { tag ->
                            FilterChip(
                                onClick = {
                                    onTagsChanged(
                                        if (tag in selectedTags) selectedTags - tag
                                        else selectedTags + tag
                                    )
                                },
                                label = { Text(tag, fontSize = 11.sp) },
                                selected = tag in selectedTags
                            )
                        }
                    }
                }
            }

            // Rating filter
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Minimum Rating: ${minRating.toInt()}★",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = minRating,
                    onValueChange = onMinRatingChange,
                    valueRange = 0f..5f,
                    steps = 4
                )
            }

            // Boolean filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked = showVerifiedOnly,
                        onCheckedChange = onShowVerifiedOnlyChange
                    )
                    Text(
                        text = "Verified only",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked = showFreeOnly,
                        onCheckedChange = onShowFreeOnlyChange
                    )
                    Text(
                        text = "Free only",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Detailed plugin list item with expandable information.
 */
@Composable
private fun PluginListItem(
    plugin: PluginInfo,
    onSelected: () -> Unit,
    onFavorited: (Boolean) -> Unit,
    onInfo: () -> Unit,
    isFavorited: Boolean,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val scale by animateFloatAsState(
        targetValue = if (isExpanded) 1.02f else 1f,
        animationSpec = tween(200),
        label = "scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable { onToggleExpanded() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isExpanded) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = plugin.pluginName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (plugin.isVerified) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Verified",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        if (plugin.isPremium) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = plugin.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "v${plugin.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = plugin.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Rating
                    if (plugin.rating > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFB000),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = String.format("%.1f", plugin.rating),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Favorite button
                    IconButton(
                        onClick = { onFavorited(!isFavorited) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorited) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorited) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Description
            Text(
                text = plugin.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            // Tags
            if (plugin.tags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(plugin.tags.take(if (isExpanded) plugin.tags.size else 3)) { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Expanded details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Divider()

                    // Statistics row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatisticItem(
                            label = "Downloads",
                            value = formatNumber(plugin.downloadCount)
                        )
                        StatisticItem(
                            label = "Parameters",
                            value = plugin.parameterCount.toString()
                        )
                        StatisticItem(
                            label = "Size",
                            value = plugin.fileSize
                        )
                        StatisticItem(
                            label = "Updated",
                            value = dateFormat.format(plugin.lastUpdated)
                        )
                    }

                    // Dependencies
                    if (plugin.dependencies.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Dependencies",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            plugin.dependencies.forEach { dependency ->
                                Text(
                                    text = "• $dependency",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Permissions
                    if (plugin.permissions.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Permissions Required",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            plugin.permissions.forEach { permission ->
                                Text(
                                    text = "• $permission",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSelected,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to Chain")
                }

                OutlinedButton(
                    onClick = onInfo,
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "More info",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Grid view plugin item.
 */
@Composable
private fun PluginGridItem(
    plugin: PluginInfo,
    onSelected: () -> Unit,
    onFavorited: (Boolean) -> Unit,
    isFavorited: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.aspectRatio(1f),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = plugin.pluginName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { onFavorited(!isFavorited) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorited) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Text(
                    text = plugin.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (plugin.rating > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB000),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = String.format("%.1f", plugin.rating),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Button(
                onClick = onSelected,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Compact plugin item for dense display.
 */
@Composable
private fun PluginCompactItem(
    plugin: PluginInfo,
    onSelected: () -> Unit,
    onFavorited: (Boolean) -> Unit,
    isFavorited: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = plugin.pluginName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (plugin.isVerified) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Verified",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = plugin.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (plugin.rating > 0) {
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB000),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = String.format("%.1f", plugin.rating),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onFavorited(!isFavorited) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorited) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Button(
                    onClick = onSelected,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Add", fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * Statistic display component.
 */
@Composable
private fun StatisticItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

/**
 * Format large numbers for display.
 */
private fun formatNumber(number: Int): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}
