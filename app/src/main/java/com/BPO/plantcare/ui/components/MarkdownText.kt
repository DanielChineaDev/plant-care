package com.BPO.plantcare.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * Renderiza markdown muy simple: **negrita**, *italica*, [texto](url) y
 * saltos de linea. Suficiente para los posts. Para algo mas rico (listas,
 * blockquotes, etc.) habria que meter una libreria de markdown completa.
 *
 * - **bold**  -> FontWeight.Bold
 * - *italic*  -> FontStyle.Italic
 * - [t](url)  -> texto subrayado clickable, abre el url en navegador
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, linkColor) { parseSimpleMarkdown(text, linkColor) }
    val uriHandler = LocalUriHandler.current

    if (annotated.getStringAnnotations(LINK_TAG, 0, annotated.length).isEmpty()) {
        // Sin links -> Text normal (mas eficiente).
        Text(text = annotated, style = style, modifier = modifier)
    } else {
        ClickableText(
            text = annotated,
            style = style.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = modifier,
            onClick = { offset ->
                annotated.getStringAnnotations(LINK_TAG, offset, offset)
                    .firstOrNull()?.item?.let { uriHandler.openUri(it) }
            },
        )
    }
}

private const val LINK_TAG = "link"

private val BOLD_REGEX = Regex("\\*\\*([^*]+)\\*\\*")
private val ITALIC_REGEX = Regex("\\*([^*]+)\\*")
private val LINK_REGEX = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")

/**
 * Convierte el markdown a AnnotatedString procesando link -> bold -> italic
 * en orden, escapando los tramos ya procesados para no doble-procesarlos.
 *
 * Implementacion deliberadamente simple: NO soporta anidados (p.ej. bold
 * dentro de link). Suficiente para 95% de los posts.
 */
internal fun parseSimpleMarkdown(input: String, linkColor: androidx.compose.ui.graphics.Color): AnnotatedString {
    data class Match(val range: IntRange, val styled: AnnotatedString)

    val matches = mutableListOf<Match>()

    LINK_REGEX.findAll(input).forEach { m ->
        val label = m.groupValues[1]
        val url = m.groupValues[2]
        matches += Match(
            m.range,
            buildAnnotatedString {
                pushStringAnnotation(LINK_TAG, url)
                withStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) { append(label) }
                pop()
            },
        )
    }
    BOLD_REGEX.findAll(input).forEach { m ->
        if (matches.none { it.range.overlapsWith(m.range) }) {
            matches += Match(
                m.range,
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.groupValues[1]) }
                },
            )
        }
    }
    ITALIC_REGEX.findAll(input).forEach { m ->
        if (matches.none { it.range.overlapsWith(m.range) }) {
            matches += Match(
                m.range,
                buildAnnotatedString {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(m.groupValues[1]) }
                },
            )
        }
    }

    matches.sortBy { it.range.first }

    return buildAnnotatedString {
        var i = 0
        for (m in matches) {
            if (m.range.first > i) append(input.substring(i, m.range.first))
            append(m.styled)
            i = m.range.last + 1
        }
        if (i < input.length) append(input.substring(i))
    }
}

private fun IntRange.overlapsWith(other: IntRange): Boolean =
    first <= other.last && other.first <= last

// Helper que evita import circular del buildAnnotatedString.withStyle.
private inline fun androidx.compose.ui.text.AnnotatedString.Builder.withStyle(
    style: SpanStyle,
    block: androidx.compose.ui.text.AnnotatedString.Builder.() -> Unit,
) {
    pushStyle(style); try { block() } finally { pop() }
}
