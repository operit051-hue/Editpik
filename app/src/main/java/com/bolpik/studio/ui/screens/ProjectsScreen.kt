package com.bolpik.studio.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bolpik.studio.i18n.LocalBolpikStrings
import com.bolpik.studio.model.BolpikProject
import com.bolpik.studio.model.ProjectStatus
import com.bolpik.studio.ui.theme.BolpikGreen
import com.bolpik.studio.ui.theme.BolpikLime
import com.bolpik.studio.ui.theme.BolpikTeal
import com.bolpik.studio.viewmodel.BolpikViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectsScreen(
    projects: List<BolpikProject>,
    viewModel: BolpikViewModel,
    onOpenProject: (BolpikProject) -> Unit,
    onExportProject: (BolpikProject) -> Unit,
    onImportNewPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalBolpikStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var longPressedProject by remember { mutableStateOf<BolpikProject?>(null) }
    var projectToRename by remember { mutableStateOf<BolpikProject?>(null) }
    var projectToDelete by remember { mutableStateOf<BolpikProject?>(null) }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 162.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.projectsTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.testTag("projects_screen_title")
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${projects.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = strings.projectsSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (projects.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(BolpikGreen.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FolderOpen,
                                    contentDescription = null,
                                    tint = BolpikGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = strings.projectsEmptyTitle,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = strings.projectsEmptySubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onImportNewPhoto,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = CircleShape,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(strings.goToStudio, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            } else {
                items(projects, key = { it.id }) { project ->
                    val statusLabel = when (project.status) {
                        ProjectStatus.IMPORTED -> strings.statusImported
                        ProjectStatus.EDITING -> "${strings.statusEditing} (${project.editCount})"
                        ProjectStatus.EXPORTED -> strings.statusExported
                    }
                    val statusColor = when (project.status) {
                        ProjectStatus.IMPORTED -> MaterialTheme.colorScheme.onSurfaceVariant
                        ProjectStatus.EDITING -> BolpikLime
                        ProjectStatus.EXPORTED -> BolpikGreen
                    }

                    val previewRequest = remember(project.id, project.updatedAtMillis, project.previewPath) {
                        val targetFile = File(project.previewPath).takeIf { it.exists() }
                            ?: File(project.originalPath)
                        ImageRequest.Builder(context)
                            .data(targetFile)
                            .memoryCacheKey("${project.id}_${project.updatedAtMillis}")
                            .diskCacheKey("${project.id}_${project.updatedAtMillis}")
                            .crossfade(true)
                            .build()
                    }

                    val originalThumbRequest = remember(project.id, project.originalPath) {
                        ImageRequest.Builder(context)
                            .data(File(project.originalPath))
                            .memoryCacheKey("orig_${project.id}")
                            .build()
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .combinedClickable(
                                onClick = { onOpenProject(project) },
                                onLongClick = { longPressedProject = project }
                            )
                            .testTag("project_card_${project.id}")
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            ) {
                                // Latest Edit Preview
                                AsyncImage(
                                    model = previewRequest,
                                    contentDescription = project.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Mini Original Photo Thumbnail PiP when edits exist
                                if (project.editCount > 0) {
                                    AsyncImage(
                                        model = originalThumbRequest,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black)
                                    )
                                }

                                // Status Pill
                                Surface(
                                    color = Color(0xCC090F0C),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.padding(11.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = dateFormatter.format(Date(project.updatedAtMillis)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BolpikTeal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Long-Press Project Actions Window (ONLY shown when user long-presses a project)
    longPressedProject?.let { project ->
        Dialog(onDismissRequest = { longPressedProject = null }) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        val thumbReq = remember(project.id, project.updatedAtMillis) {
                            ImageRequest.Builder(context)
                                .data(File(project.previewPath).takeIf { it.exists() } ?: File(project.originalPath))
                                .memoryCacheKey("dlg_${project.id}_${project.updatedAtMillis}")
                                .build()
                        }
                        AsyncImage(
                            model = thumbReq,
                            contentDescription = project.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = strings.projectActionsTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    ProjectActionRow(
                        icon = Icons.Filled.AutoFixHigh,
                        label = strings.continueEditing,
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = {
                            longPressedProject = null
                            onOpenProject(project)
                        }
                    )
                    ProjectActionRow(
                        icon = Icons.Filled.Edit,
                        label = strings.renameProject,
                        tint = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            longPressedProject = null
                            projectToRename = project
                        }
                    )
                    ProjectActionRow(
                        icon = Icons.Filled.FileDownload,
                        label = strings.exportEditedImage,
                        tint = BolpikLime,
                        onClick = {
                            longPressedProject = null
                            onExportProject(project)
                        }
                    )
                    ProjectActionRow(
                        icon = Icons.Filled.Share,
                        label = strings.shareFinalResult,
                        tint = BolpikTeal,
                        onClick = {
                            longPressedProject = null
                            scope.launch {
                                val uri = viewModel.createShareUriForProject(project)
                                if (uri != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/jpeg"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, strings.shareFinalResult)
                                    )
                                }
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ProjectActionRow(
                        icon = Icons.Filled.DeleteOutline,
                        label = strings.deleteProject,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            longPressedProject = null
                            projectToDelete = project
                        }
                    )
                }
            }
        }
    }

    // Rename Project Dialog
    projectToRename?.let { target ->
        var newName by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = { Text(strings.renameProject) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(strings.renameProjectLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameProject(target.id, newName)
                        projectToRename = null
                    },
                    enabled = newName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(strings.save, color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) { Text(strings.cancel) }
            }
        )
    }

    // Delete Confirmation Dialog
    projectToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text(strings.deleteProjectConfirmTitle) },
            text = { Text(strings.deleteProjectConfirmBody) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(target.id)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun ProjectActionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = tint)
        }
    }
}
