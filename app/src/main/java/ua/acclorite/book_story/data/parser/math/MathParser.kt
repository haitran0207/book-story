/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.math

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import ua.acclorite.book_story.domain.model.reader.ReaderText
import java.util.concurrent.atomic.AtomicInteger

object MathParser {

    private val idCounter = AtomicInteger(0)

    /**
     * Processes MathML and LaTeX math tags within the HTML document.
     * Replaces block/complex math (matrices, multiline equations, fractions) with formula tokens,
     * and converts simple inline math to clean Unicode.
     *
     * @param document Jsoup parsed document.
     * @return Map of formula tokens to their rendered [ReaderText.Formula] representations.
     */
    fun processMathInDocument(document: Document): Map<String, ReaderText.Formula> {
        val formulasMap = mutableMapOf<String, ReaderText.Formula>()

        // 1. Process all <math> elements (EPUB 3 / MathML standard)
        document.select("math").forEach { mathElement ->
            processMathElement(mathElement, formulasMap)
        }

        // 2. Process MathJax / KaTeX span or div containers if any
        document.select("span.math, div.math, span.tex, div.tex").forEach { mathElement ->
            if (mathElement.select("math").isEmpty()) {
                processMathElement(mathElement, formulasMap)
            }
        }

        return formulasMap
    }

    private fun processMathElement(
        element: Element,
        formulasMap: MutableMap<String, ReaderText.Formula>
    ) {
        val latex = extractLatexFromElement(element)
        if (latex.isBlank()) {
            // If completely empty, remove element
            element.remove()
            return
        }

        val isBlock = isBlockOrComplexFormula(element, latex)

        if (isBlock) {
            val bitmap = MathRenderer.renderLatex(latex)
            if (bitmap != null) {
                val id = "FORMULA_${idCounter.incrementAndGet()}"
                formulasMap[id] = ReaderText.Formula(
                    imageBitmap = bitmap,
                    latex = latex
                )
                // Insert distinct block formula marker
                element.replaceWith(TextNode("\n[[MATH_BLOCK|$id]]\n"))
                return
            }
        }

        // Inline or fallback to Unicode
        val unicode = LatexToUnicode.convert(latex)
        element.replaceWith(TextNode(" $unicode "))
    }

    /**
     * Determines whether the formula should be rendered as a dedicated graphic block.
     */
    private fun isBlockOrComplexFormula(element: Element, latex: String): Boolean {
        // 1. Explicit display attribute from EPUB MathML
        if (element.attr("display").equals("block", ignoreCase = true)) return true
        if (element.attr("display").equals("inline", ignoreCase = true)) return false

        // 2. Container tag or class
        if (element.tagName().equals("div", ignoreCase = true)) return true
        if (element.hasClass("display")) return true

        // 3. Standalone child in a paragraph
        val parent = element.parent()
        if (parent != null && parent.tagName().equals("p", ignoreCase = true)) {
            val ownText = parent.ownText().trim()
            val otherChildren = parent.children().filter { it != element }
            if (ownText.isEmpty() && otherChildren.isEmpty()) {
                return true
            }
        }

        // 4. Multiline LaTeX environments
        if (latex.contains("\\begin{align") || latex.contains("\\begin{equation") || latex.contains("\\begin{gather")) return true

        return false
    }

    /**
     * Extracts LaTeX from <annotation> or converts MathML DOM tree into LaTeX.
     */
    private fun extractLatexFromElement(element: Element): String {
        // 1. Direct annotation tag (<annotation encoding="application/x-tex">)
        val annotation = element.selectFirst("annotation")
        if (annotation != null && annotation.text().isNotBlank()) {
            return annotation.text().trim()
        }

        // 2. Data attributes (e.g. data-tex, data-latex)
        val dataTex = element.attr("data-tex").ifBlank { element.attr("data-latex") }
        if (dataTex.isNotBlank()) {
            return dataTex.trim()
        }

        // 3. Fallback: Parse MathML DOM into LaTeX
        return mathmlToLatex(element).trim()
    }

    /**
     * Converts standard MathML DOM elements into LaTeX syntax.
     */
    private fun mathmlToLatex(element: Element): String {
        return when (element.tagName().lowercase()) {
            "mi", "mn", "mo" -> element.text()
            "mtext" -> "\\text{${element.text()}}"
            "mfrac" -> {
                val children = element.children()
                val num = if (children.size > 0) mathmlToLatex(children[0]) else ""
                val den = if (children.size > 1) mathmlToLatex(children[1]) else ""
                "\\frac{$num}{$den}"
            }
            "msqrt" -> {
                val inner = element.children().joinToString(" ") { mathmlToLatex(it) }
                "\\sqrt{$inner}"
            }
            "mroot" -> {
                val children = element.children()
                val base = if (children.size > 0) mathmlToLatex(children[0]) else ""
                val root = if (children.size > 1) mathmlToLatex(children[1]) else ""
                "\\sqrt[$root]{$base}"
            }
            "msup" -> {
                val children = element.children()
                val base = if (children.size > 0) mathmlToLatex(children[0]) else ""
                val sup = if (children.size > 1) mathmlToLatex(children[1]) else ""
                "{$base}^{$sup}"
            }
            "msub" -> {
                val children = element.children()
                val base = if (children.size > 0) mathmlToLatex(children[0]) else ""
                val sub = if (children.size > 1) mathmlToLatex(children[1]) else ""
                "{$base}_{$sub}"
            }
            "msubsup" -> {
                val children = element.children()
                val base = if (children.size > 0) mathmlToLatex(children[0]) else ""
                val sub = if (children.size > 1) mathmlToLatex(children[1]) else ""
                val sup = if (children.size > 2) mathmlToLatex(children[2]) else ""
                "{$base}_{$sub}^{$sup}"
            }
            "mtable" -> {
                val rows = element.select("mtr, tr").map { row ->
                    row.select("mtd, td").joinToString(" & ") { mathmlToLatex(it) }
                }.joinToString(" \\\\ ")
                "\\begin{bmatrix} $rows \\end{bmatrix}"
            }
            "mtr", "tr" -> element.children().joinToString(" & ") { mathmlToLatex(it) }
            "mtd", "td" -> element.children().joinToString(" ") { mathmlToLatex(it) }
            else -> element.children().joinToString(" ") { mathmlToLatex(it) }.ifBlank { element.text() }
        }
    }
}
