/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.math

object LatexToUnicode {

    private val GREEK_AND_SYMBOLS = mapOf(
        "\\mathbb{R}" to "ℝ",
        "\\mathbb{C}" to "ℂ",
        "\\mathbb{N}" to "ℕ",
        "\\mathbb{Z}" to "ℤ",
        "\\mathbb{Q}" to "ℚ",
        "\\mathbb{H}" to "ℍ",
        "\\mathcal{H}" to "ℋ",
        "\\alpha" to "α",
        "\\beta" to "β",
        "\\gamma" to "γ",
        "\\delta" to "δ",
        "\\epsilon" to "ε",
        "\\varepsilon" to "ε",
        "\\zeta" to "ζ",
        "\\eta" to "η",
        "\\theta" to "θ",
        "\\vartheta" to "ϑ",
        "\\iota" to "ι",
        "\\kappa" to "κ",
        "\\lambda" to "λ",
        "\\mu" to "μ",
        "\\nu" to "ν",
        "\\xi" to "ξ",
        "\\pi" to "π",
        "\\varpi" to "ϖ",
        "\\rho" to "ρ",
        "\\varrho" to "ϱ",
        "\\sigma" to "σ",
        "\\varsigma" to "ς",
        "\\tau" to "τ",
        "\\upsilon" to "υ",
        "\\phi" to "ϕ",
        "\\varphi" to "φ",
        "\\chi" to "χ",
        "\\psi" to "ψ",
        "\\omega" to "ω",
        "\\Gamma" to "Γ",
        "\\Delta" to "Δ",
        "\\Theta" to "Θ",
        "\\Lambda" to "Λ",
        "\\Xi" to "Ξ",
        "\\Pi" to "Π",
        "\\Sigma" to "Σ",
        "\\Upsilon" to "Υ",
        "\\Phi" to "Φ",
        "\\Psi" to "Ψ",
        "\\Omega" to "Ω",
        "\\uparrow" to "↑",
        "\\downarrow" to "↓",
        "\\hbar" to "ℏ",
        "\\times" to "×",
        "\\cdot" to "·",
        "\\approx" to "≈",
        "\\neq" to "≠",
        "\\ne" to "≠",
        "\\le" to "≤",
        "\\leq" to "≤",
        "\\ge" to "≥",
        "\\geq" to "≥",
        "\\pm" to "±",
        "\\mp" to "∓",
        "\\infty" to "∞",
        "\\in" to "∈",
        "\\notin" to "∉",
        "\\subset" to "⊂",
        "\\subseteq" to "⊆",
        "\\supset" to "⊃",
        "\\supseteq" to "⊇",
        "\\oplus" to "⊕",
        "\\otimes" to "⊗",
        "\\odot" to "⊙",
        "\\circ" to "∘",
        "\\bullet" to "•",
        "\\dagger" to "†",
        "\\ddagger" to "‡",
        "\\dots" to "…",
        "\\cdots" to "⋯",
        "\\vdots" to "…",
        "\\ddots" to "…",
        "\\to" to "→",
        "\\rightarrow" to "→",
        "\\leftarrow" to "←",
        "\\leftrightarrow" to "↔",
        "\\Rightarrow" to "⇒",
        "\\Leftarrow" to "⇐",
        "\\Leftrightarrow" to "⇔",
        "\\forall" to "∀",
        "\\exists" to "∃",
        "\\nabla" to "∇",
        "\\partial" to "∂",
        "\\sum" to "∑",
        "\\prod" to "∏",
        "\\int" to "∫",
        "\\parallel" to "∥",
        "\\Vert" to "∥",
        "\\|" to "∥",
        "\\sim" to "∼",
        "\\equiv" to "≡",
        "\\quad" to " ",
        "\\qquad" to "  ",
        "\\," to " ",
        "\\;" to " ",
        "\\:" to " ",
        "\\!" to ""
    )

    private val SUPERSCRIPT_MAP = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ', 'T' to 'ᵀ', 'k' to 'ᵏ', 'm' to 'ᵐ'
    )

    private val SUBSCRIPT_MAP = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
        'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ',
        'v' to 'ᵥ', 'x' to 'ₓ'
    )

    /**
     * Converts a LaTeX formula or symbol into clean, human-readable Unicode text.
     */
    fun convert(latex: String): String {
        var s = latex.trim()

        // Strip enclosing dollar signs
        if (s.startsWith("$$") && s.endsWith("$$") && s.length >= 4) {
            s = s.substring(2, s.length - 2).trim()
        } else if (s.startsWith("$") && s.endsWith("$") && s.length >= 2) {
            s = s.substring(1, s.length - 1).trim()
        }

        // Dirac and bracket delimiters
        s = s.replace(Regex("""\\left\|\s*"""), "|")
        s = s.replace(Regex("""\\right\|\s*"""), "|")
        s = s.replace(Regex("""\\left\\langle\s*"""), "⟨")
        s = s.replace(Regex("""\\right\\rangle\s*"""), "⟩")
        s = s.replace(Regex("""\\left\s*\|"""), "|")
        s = s.replace(Regex("""\\right\s*\|"""), "|")
        s = s.replace(Regex("""\\left\s*<"""), "⟨")
        s = s.replace(Regex("""\\right\s*>"""), "⟩")
        s = s.replace(Regex("""\\left\s*\("""), "(")
        s = s.replace(Regex("""\\right\s*\)"""), ")")
        s = s.replace(Regex("""\\left\s*\["""), "[")
        s = s.replace(Regex("""\\right\s*\]"""), "]")
        s = s.replace(Regex("""\\left\s*\{"""), "{")
        s = s.replace(Regex("""\\right\s*\}"""), "}")
        s = s.replace(Regex("""\\langle\s*"""), "⟨")
        s = s.replace(Regex("""\\rangle\s*"""), "⟩")
        s = s.replace(Regex("""\\\|\s*"""), "∥")
        s = s.replace(Regex("""\\vert\s*"""), "|")
        s = s.replace(Regex("""\\mid\s*"""), "|")

        // Matrix / column vector fallback in inline text: [ a \\ b ] -> [a, b]ᵀ or [a b]
        s = s.replace(Regex("""\\begin\{[bp]?matrix\}([\s\S]*?)\\end\{[bp]?matrix\}""")) { match ->
            val inner = match.groupValues[1].trim()
            val rows = inner.split(Regex("""\\\\""")).map { row ->
                row.split("&").joinToString(" ") { convert(it).trim() }.trim()
            }.filter { it.isNotBlank() }
            if (rows.size == 1) {
                "[${rows[0]}]"
            } else {
                "[${rows.joinToString(", ")}]ᵀ"
            }
        }

        // Greek and Math symbols
        for ((k, v) in GREEK_AND_SYMBOLS) {
            s = s.replace(k, v)
        }

        // Text blocks
        s = s.replace(Regex("""\\text\{([^}]*)\}"""), "$1")
        s = s.replace(Regex("""\\mathrm\{([^}]*)\}"""), "$1")
        s = s.replace(Regex("""\\operatorname\{([^}]*)\}"""), "$1")
        s = s.replace(Regex("""\\textbf\{([^}]*)\}"""), "**$1**")
        s = s.replace(Regex("""\\mathbf\{([^}]*)\}"""), "**$1**")
        s = s.replace(Regex("""\\textit\{([^}]*)\}"""), "_$1_")
        s = s.replace(Regex("""\\mathit\{([^}]*)\}"""), "_$1_")

        // Fractions: \frac{a}{b} -> a/b
        s = s.replace(Regex("""\\frac\{([^{}]+)\}\{([^{}]+)\}""")) { match ->
            val num = convert(match.groupValues[1]).trim()
            val den = convert(match.groupValues[2]).trim()
            "$num/$den"
        }

        // Square roots: \sqrt{10} -> √10
        s = s.replace(Regex("""\\sqrt\[3\]\{([^{}]+)\}""")) { "∛${convert(it.groupValues[1])}" }
        s = s.replace(Regex("""\\sqrt\{([^{}]+)\}""")) { "√${convert(it.groupValues[1])}" }

        // Superscripts
        s = s.replace(Regex("""\^\{?([0-9\+\-\(\)niTkmb]+)\}?""")) { match ->
            val content = match.groupValues[1]
            val converted = content.map { SUPERSCRIPT_MAP[it] ?: it }.joinToString("")
            if (converted.all { it in SUPERSCRIPT_MAP.values }) converted else "^$content"
        }
        s = s.replace(Regex("""\^\{\\dagger\}"""), "†")
        s = s.replace(Regex("""\^\\dagger"""), "†")

        // Subscripts
        s = s.replace(Regex("""_\{?([0-9aehijklmnoprstuvx\+\-\(\)]+)\}?""")) { match ->
            val content = match.groupValues[1]
            val converted = content.map { SUBSCRIPT_MAP[it] ?: it }.joinToString("")
            if (converted.all { it in SUBSCRIPT_MAP.values }) converted else "_$content"
        }

        // Clean leftover TeX commands
        s = s.replace(Regex("""\\([a-zA-Z]+)"""), "$1")
        s = s.replace(Regex("""\{([^}]*)\}"""), "$1")
        s = s.replace(Regex("""\s+"""), " ").trim()

        return s
    }
}
