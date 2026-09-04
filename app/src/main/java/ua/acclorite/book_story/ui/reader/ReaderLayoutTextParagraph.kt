/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.domain.model.reader.ReaderText.Text
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.presentation.reader.model.ReaderFontThickness
import ua.acclorite.book_story.presentation.reader.model.ReaderTextAlignment
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.common.helpers.noRippleClickable
import ua.acclorite.book_story.ui.reader.model.FontWithName

@Composable
fun LazyItemScope.ReaderLayoutTextParagraph(
    paragraph: Text,
    showMenu: Boolean,
    fontFamily: FontWithName,
    fontColor: Color,
    lineHeight: TextUnit,
    fontThickness: ReaderFontThickness,
    fontStyle: FontStyle,
    textAlignment: ReaderTextAlignment,
    horizontalAlignment: Alignment.Horizontal,
    fontSize: TextUnit,
    letterSpacing: TextUnit,
    sidePadding: Dp,
    paragraphIndentation: TextUnit,
    doubleClickTranslation: Boolean,
    highlightedReading: Boolean,
    highlightedReadingThickness: FontWeight,
    toolbarHidden: Boolean,
    openTranslator: (ReaderEvent.OnOpenTranslator) -> Unit,
    menuVisibility: (ReaderEvent.OnMenuVisibility) -> Unit,
    isReading: Boolean = false
) {
    val highlightBgColor by animateColorAsState(
        targetValue = if (isReading) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        } else {
            Color.Transparent
        },
        label = "readAloudHighlightBg"
    )

    val highlightBorderColor by animateColorAsState(
        targetValue = if (isReading) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        } else {
            Color.Transparent
        },
        label = "readAloudHighlightBorder"
    )

    Column(
        modifier = Modifier
            .animateItem(fadeInSpec = null, fadeOutSpec = null)
            .fillMaxWidth()
            .padding(horizontal = sidePadding)
            .clip(RoundedCornerShape(8.dp))
            .background(highlightBgColor)
            .border(
                width = if (isReading) 1.5.dp else 0.dp,
                color = highlightBorderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = if (isReading) 10.dp else 0.dp,
                vertical = if (isReading) 6.dp else 0.dp
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = horizontalAlignment
    ) {
        StyledText(
            text = paragraph.line,
            modifier = Modifier.then(
                if (doubleClickTranslation && toolbarHidden) {
                    Modifier.noRippleClickable(
                        onDoubleClick = {
                            openTranslator(
                                ReaderEvent.OnOpenTranslator(
                                    textToTranslate = paragraph.line.text,
                                    translateWholeParagraph = true
                                )
                            )
                        },
                        onClick = {
                            menuVisibility(
                                ReaderEvent.OnMenuVisibility(
                                    show = !showMenu,
                                    saveCheckpoint = true
                                )
                            )
                        }
                    )
                } else Modifier
            ),
            style = TextStyle(
                fontFamily = fontFamily.font,
                fontWeight = fontThickness.thickness,
                textAlign = textAlignment.textAlignment,
                textIndent = TextIndent(firstLine = paragraphIndentation),
                fontStyle = fontStyle,
                letterSpacing = letterSpacing,
                fontSize = fontSize,
                lineHeight = lineHeight,
                color = fontColor,
                lineBreak = LineBreak.Paragraph
            ),
            highlightText = highlightedReading,
            highlightThickness = highlightedReadingThickness
        )
    }
}