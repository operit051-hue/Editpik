package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.PresetsCatalog
import com.example.engine.ImageProcessor
import com.example.i18n.LocalBolpikStrings
import com.example.model.FilterCategory
import com.example.model.PresetRecipe
import com.example.ui.theme.BolpikGreen
import com.example.ui.theme.BolpikLime
import com.example.ui.theme.BolpikTeal
import com.example.viewmodel.BolpikViewModel
import com.example.viewmodel.PresetsUiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PresetsScreen(
    state: PresetsUiState,
    customPresets: List<PresetRecipe>,
    viewModel: BolpikViewModel,
    onApplyInEditor: (PresetRecipe) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalBolpikStrings.current
    val isEn = strings.navHome == "Home"
    var showOriginal by remember { mutableStateOf(false) }
    var presetToDelete by remember { mutableStateOf<PresetRecipe?>(null) }

    val allRecipes = remember(customPresets) {
        customPresets + PresetsCatalog.builtInRecipes
    }
    val filteredRecipes = remember(allRecipes, state.selectedCategory) {
        if (state.selectedCategory == FilterCategory.ALL) allRecipes
        else allRecipes.filter { it.category == state.selectedCategory }
    }

    val pagerRecipes = if (filteredRecipes.isNotEmpty()) filteredRecipes else allRecipes
    val pagerState = rememberPagerState(pageCount = { pagerRecipes.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage, pagerRecipes) {
        pagerRecipes.getOrNull(pagerState.currentPage)?.let { recipe ->
            viewModel.selectPresetRecipe(recipe.id)
        }
    }

    val activeRecipe = remember(pagerRecipes, state.selectedRecipeId) {
        pagerRecipes.firstOrNull { it.id == state.selectedRecipeId } ?: pagerRecipes.first()
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.setPresetsPreviewFromUri(uri)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 800.dp),
            contentPadding = PaddingValues(bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.presetsTitle,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = strings.presetsSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = CircleShape
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.photoButton)
                    }
                }
            }

            // Swipeable Page View (HorizontalPager)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        pageSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val recipe = pagerRecipes[page]
                        val cf = remember(recipe, showOriginal) {
                            if (showOriginal) null
                            else ColorFilter.colorMatrix(
                                ColorMatrix(
                                    ImageProcessor.buildCombinedColorMatrixArray(
                                        recipe.adjustments,
                                        recipe.filterId,
                                        recipe.filterIntensity
                                    )
                                )
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(
                                1.dp,
                                if (pagerState.currentPage == page) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp)
                                ) {
                                    state.previewBitmap?.let { bmp ->
                                        Image(
                                            bitmap = remember(bmp) { bmp.asImageBitmap() },
                                            contentDescription = recipe.name,
                                            contentScale = ContentScale.Crop,
                                            colorFilter = cf,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Surface(
                                        color = Color(0xCC090F0C),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = if (showOriginal) "ORIGINAL" else recipe.name.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (showOriginal) Color.White else BolpikGreen,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                    Surface(
                                        color = if (showOriginal) BolpikTeal else Color(0xCC090F0C),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp)
                                            .clip(CircleShape)
                                            .clickable { showOriginal = !showOriginal }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Compare,
                                                contentDescription = "Comparar original",
                                                tint = if (showOriginal) Color.Black else Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (showOriginal) strings.originalLabel else strings.compareLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (showOriginal) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            recipe.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    Button(
                                        onClick = { onApplyInEditor(recipe) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = CircleShape,
                                        modifier = Modifier.testTag("presets_apply_button")
                                    ) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(strings.usePreset, color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Pager Dots Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(pagerRecipes.size) { index ->
                            val active = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(width = if (active) 18.dp else 7.dp, height = 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    )
                            )
                        }
                    }
                }
            }

            // Sample Switcher + Category Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PresetsCatalog.samplePhotos) { s ->
                            FilterChip(
                                selected = state.previewTitle == s.title,
                                onClick = { viewModel.setPresetsPreviewFromSample(s) },
                                label = { Text("Muestra: ${s.category}") }
                            )
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(FilterCategory.entries) { cat ->
                            FilterChip(
                                selected = state.selectedCategory == cat,
                                onClick = { viewModel.selectPresetCategory(cat) },
                                label = { Text(cat.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Expandable Recipe Cards with RadioButton Selection
            if (filteredRecipes.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                if (isEn) "No custom presets saved yet" else "Sin presets guardados aún",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                if (isEn) "Save any combination from the Editor menu to reuse it here."
                                else "Guarda cualquier combinación desde el menú del Editor para reutilizarla aquí.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredRecipes, key = { it.id }) { recipe ->
                    val selected = recipe.id == activeRecipe.id
                    val filterMeta = PresetsCatalog.findFilter(recipe.filterId)
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.selectPresetRecipe(recipe.id)
                                val targetIdx = pagerRecipes.indexOfFirst { it.id == recipe.id }
                                if (targetIdx >= 0) {
                                    scope.launch { pagerState.animateScrollToPage(targetIdx) }
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = {
                                        viewModel.selectPresetRecipe(recipe.id)
                                        val targetIdx = pagerRecipes.indexOfFirst { it.id == recipe.id }
                                        if (targetIdx >= 0) {
                                            scope.launch { pagerState.animateScrollToPage(targetIdx) }
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${filterMeta.name} ${(recipe.filterIntensity * 100).roundToInt()}% • ${recipe.category.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (recipe.isCustom) {
                                    IconButton(onClick = { presetToDelete = recipe }) {
                                        Icon(
                                            Icons.Filled.DeleteOutline,
                                            contentDescription = strings.delete,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(visible = selected) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 48.dp, top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TelemetryPill(if (isEn) "Contrast ${recipe.adjustments.contrast.roundToInt()}" else "Contraste ${recipe.adjustments.contrast.roundToInt()}")
                                    TelemetryPill(if (isEn) "Warmth ${recipe.adjustments.warmth.roundToInt()}" else "Calidez ${recipe.adjustments.warmth.roundToInt()}")
                                    TelemetryPill(if (isEn) "Vignette ${recipe.adjustments.vignette.roundToInt()}" else "Viñeta ${recipe.adjustments.vignette.roundToInt()}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    presetToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = { Text(if (isEn) "Delete '${target.name}'?" else "¿Eliminar '${target.name}'?") },
            text = { Text(if (isEn) "This custom recipe will be removed from your studio." else "Esta receta personalizada se borrará de tu estudio.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomPreset(target.id)
                        presetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { presetToDelete = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun TelemetryPill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = BolpikLime,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
