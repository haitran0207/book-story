/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.browse

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.common.helpers.noRippleClickable

@Composable
fun BrowseLayoutHeader(
    header: String,
    pinned: Boolean,
    collapsed: Boolean,
    fileCount: Int,
    pin: () -> Unit,
    toggleCollapse: () -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (collapsed) -90f else 0f,
        label = "collapse_arrow_rotation"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .noRippleClickable {
                    toggleCollapse()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(
                    if (collapsed) R.string.expand_content_desc
                    else R.string.collapse_content_desc
                ),
                modifier = Modifier
                    .size(20.dp)
                    .rotate(arrowRotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StyledText(
                text = "$header ($fileCount)",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
        Icon(
            modifier = Modifier
                .size(20.dp)
                .noRippleClickable {
                    pin()
                },
            imageVector = if (pinned) Icons.Default.PushPin
            else Icons.Outlined.PushPin,
            contentDescription = stringResource(R.string.pin_content_desc),
            tint = if (pinned) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}