/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.settings.general.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.getAbsolutePath
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import ua.acclorite.book_story.R
import ua.acclorite.book_story.data.cache.BookTextCacheManager
import ua.acclorite.book_story.domain.model.settings.CacheLocation
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.common.components.settings.SegmentedButtonWithTitle
import ua.acclorite.book_story.ui.common.helpers.LocalSettings
import ua.acclorite.book_story.ui.common.model.ListItem

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CacheLocationEntryPoint {
    fun bookTextCacheManager(): BookTextCacheManager
}

@Composable
fun CacheLocationOption() {
    val context = LocalContext.current
    val bookTextCacheManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            CacheLocationEntryPoint::class.java
        ).bookTextCacheManager()
    }

    val settings = LocalSettings.current
    val scope = rememberCoroutineScope()
    val isSdCardAvailable = bookTextCacheManager.isSdCardAvailable()
    val currentLocation = settings.cacheLocation.value

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val docFile = DocumentFileCompat.fromUri(context, uri)
        val absPath = docFile?.getAbsolutePath(context) ?: uri.path.orEmpty()

        scope.launch {
            bookTextCacheManager.moveCache(
                targetLocation = CacheLocation.SD_CARD,
                customPath = absPath,
                customUri = uri.toString()
            )
        }
    }

    val internalTitle = stringResource(id = R.string.cache_location_internal)
    val sdCardTitle = if (isSdCardAvailable) {
        stringResource(id = R.string.cache_location_sd_card)
    } else {
        stringResource(id = R.string.cache_location_sd_card_unavailable)
    }

    val options = listOf(
        ListItem(
            item = CacheLocation.INTERNAL,
            title = internalTitle,
            selected = currentLocation == CacheLocation.INTERNAL
        ),
        ListItem(
            item = CacheLocation.SD_CARD,
            title = sdCardTitle,
            selected = currentLocation == CacheLocation.SD_CARD
        )
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        SegmentedButtonWithTitle(
            title = stringResource(id = R.string.cache_location_option),
            buttons = options,
            onClick = { targetLocation ->
                if (targetLocation == CacheLocation.SD_CARD && !isSdCardAvailable) {
                    return@SegmentedButtonWithTitle
                }
                if (targetLocation != currentLocation) {
                    scope.launch {
                        bookTextCacheManager.moveCache(targetLocation)
                    }
                }
            }
        )

        AnimatedVisibility(visible = currentLocation == CacheLocation.SD_CARD) {
            val currentPathDisplay = settings.customCachePath.value.ifBlank {
                bookTextCacheManager.getCacheDir(CacheLocation.SD_CARD).absolutePath
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    StyledText(
                        text = stringResource(id = R.string.current_cache_path, currentPathDisplay),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            folderPickerLauncher.launch(null)
                        }
                        .padding(vertical = 4.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    StyledText(
                        text = stringResource(id = R.string.change_cache_folder),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}
