/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.math

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import ru.noties.jlatexmath.JLatexMathDrawable
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.core.log.logI

object MathRenderer {

    private const val TAG = "MathRenderer"

    /**
     * Renders a LaTeX formula into an [ImageBitmap] with transparent background and white glyphs.
     * The white glyphs allow Jetpack Compose to apply dynamic color tinting based on the active reader theme.
     *
     * @param latex LaTeX formula string.
     * @param textSize Text size in sp/dp for rendering (default 22f).
     * @return [ImageBitmap] or null if rendering fails.
     */
    fun renderLatex(
        latex: String,
        textSize: Float = 24f
    ): ImageBitmap? {
        return try {
            var formula = latex.trim()
            if (formula.startsWith("$$") && formula.endsWith("$$") && formula.length >= 4) {
                formula = formula.substring(2, formula.length - 2).trim()
            } else if (formula.startsWith("$") && formula.endsWith("$") && formula.length >= 2) {
                formula = formula.substring(1, formula.length - 1).trim()
            }

            if (formula.isBlank()) return null

            // Pre-process common variations for JLatexMath compatibility
            formula = preprocessLatexForRenderer(formula)

            val drawable = JLatexMathDrawable.builder(formula)
                .textSize(textSize * 2.5f) // High-density scaling for crisp display
                .color(Color.WHITE)
                .align(JLatexMathDrawable.ALIGN_CENTER)
                .padding(16, 12, 16, 12)
                .build()

            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)

            bitmap.asImageBitmap().apply {
                prepareToDraw()
            }
        } catch (e: Throwable) {
            logE(TAG, "Failed to render LaTeX '$latex': ${e.message}")
            null
        }
    }

    /**
     * Cleans and normalizes LaTeX string before passing to JLatexMath renderer.
     */
    private fun preprocessLatexForRenderer(latex: String): String {
        var s = latex.trim()

        // Replace HTML entities inside LaTeX if any
        s = s.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")

        // Replace \left| ... \right| issues if asymmetric
        s = s.replace(Regex("""\\left\|\s*"""), "\\left| ")
        s = s.replace(Regex("""\\right\|\s*"""), "\\right| ")

        return s
    }
}
