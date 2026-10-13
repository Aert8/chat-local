package com.example.modelolocal.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import android.view.View
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.modelolocal.ui.theme.ChatColors
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import ru.wertik.orcex.font.stix2.StixTwoMath
import ru.wertik.orcex.layout.MathLayout
import ru.wertik.orcex.render.android.AndroidLatexEngine
import ru.wertik.orcex.render.android.CanvasMathRenderer

/**
 * Renderizador robusto de Markdown + LaTeX.
 *
 * Características:
 * - Markdown completo GFM: headings, bold, italic, strikethrough, listas, tablas,
 *   checklists, blockquotes, code fences con syntax highlighting, links, imágenes
 * - LaTeX: $$...$$, $...$, \[...\], \(...\) con fallback graceful y soporte para expresiones anchas
 * - Emojis Unicode: pasan directo sin alteración
 * - Tipografía proporcional y congruente: h1(20sp) -> h6/body(14sp) armonizados con LaTeX (14sp)
 * - Ancho completo con scroll horizontal para fórmulas y tablas extensas
 */
@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier
) {
    val segments = rememberContentSegments(content)
    val bodyFontSize = MaterialTheme.typography.bodyMedium.fontSize

    val colors = chatMarkdownColors()
    val typography = chatMarkdownTypography()
    val components = chatMarkdownComponents()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        segments.forEach { segment ->
            when (segment) {
                is ContentSegment.Text -> {
                    if (segment.value.isNotBlank()) {
                        Markdown(
                            content = segment.value,
                            modifier = Modifier.fillMaxWidth(),
                            colors = colors,
                            typography = typography,
                            components = components
                        )
                    }
                }
                is ContentSegment.Math -> {
                    LatexFormula(
                        formula = segment.value,
                        fontSize = bodyFontSize,
                        isDisplay = segment.isDisplay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (segment.isDisplay) 4.dp else 2.dp)
                    )
                }
            }
        }
    }
}

// ─── Colores Markdown ────────────────────────────────────────────────────────

@Composable
private fun chatMarkdownColors() = markdownColor(
    text = ChatColors.AssistantMessageText,
    codeBackground = ChatColors.CodeBlockBackground,
    inlineCodeBackground = ChatColors.InlineCodeBackground,
    dividerColor = ChatColors.DividerColor,
)

// ─── Tipografía Markdown (proporcional y congruente) ─────────────────────────
//
// Escala de tamaños diseñada para que ningún elemento se vea
// desproporcionado respecto a otro:
//   texto base:  14sp
//   code:        13sp (monospace)
//   h6 / h5:     14sp (medium)
//   h4:          15sp (medium)
//   h3:          16sp (semi-bold)
//   h2:          18sp (semi-bold)
//   h1:          20sp (bold, prominente pero congruente)
//   quote:       14sp
//
// LaTeX usa el mismo bodyMedium fontSize (14sp) como base,
// asegurando armonía visual entre texto y matemáticas.

@Composable
private fun chatMarkdownTypography() = markdownTypography(
    h1 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        color = Color(0xFFF0F0F0)
    ),
    h2 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        color = Color(0xFFECECEC)
    ),
    h3 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = Color(0xFFE8E8E8)
    ),
    h4 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = Color(0xFFE4E4E4)
    ),
    h5 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = Color(0xFFE0E0E0)
    ),
    h6 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = Color(0xFFD8D8D8)
    ),
    text = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = ChatColors.AssistantMessageText
    ),
    code = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = ChatColors.CodeText
    ),
    inlineCode = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = ChatColors.InlineCodeText
    ),
    quote = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = ChatColors.BlockquoteText
    ),
    paragraph = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = ChatColors.AssistantMessageText
    ),
    ordered = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = ChatColors.AssistantMessageText
    ),
    bullet = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = ChatColors.AssistantMessageText
    ),
    list = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = ChatColors.AssistantMessageText
    ),
)

// ─── Componentes Markdown customizados ───────────────────────────────────────

@Composable
private fun chatMarkdownComponents() = markdownComponents(
    codeBlock = highlightedCodeBlock,
    codeFence = highlightedCodeFence
)

// ─── Parser de segmentos: Markdown vs LaTeX ──────────────────────────────────
//
// Extrae bloques de LaTeX del contenido sin tocar el contenido
// que está dentro de bloques de código (```, `) ni en streaming activo.
// Patrones soportados: $$...$$, \[...\], \(...\), $...$

private sealed class ContentSegment {
    data class Text(val value: String) : ContentSegment()
    data class Math(val value: String, val isDisplay: Boolean) : ContentSegment()
}

@Composable
private fun rememberContentSegments(content: String): List<ContentSegment> {
    return remember(content) {
        parseContentSegments(content)
    }
}

private fun parseContentSegments(content: String): List<ContentSegment> {
    if (content.isBlank()) return listOf(ContentSegment.Text(content))

    // Paso 1: Identificar regiones protegidas (bloques de código fenced, inline y streaming)
    val codeBlockRegex = Regex("```[\\s\\S]*?```")
    val inlineCodeRegex = Regex("`[^`\n]+`")
    val unclosedCodeRegex = Regex("```[\\s\\S]*$")
    val protectedRanges = mutableListOf<IntRange>()
    codeBlockRegex.findAll(content).forEach { protectedRanges.add(it.range) }
    inlineCodeRegex.findAll(content).forEach { protectedRanges.add(it.range) }
    unclosedCodeRegex.findAll(content).forEach { protectedRanges.add(it.range) }

    fun isProtected(pos: Int) = protectedRanges.any { pos in it }

    // Paso 2: Buscar LaTeX solo fuera de regiones protegidas
    val mathPattern = Regex(
        """(\$\$[\s\S]+?\$\$)""" +                                  // $$...$$ display math
        """|(\\\[[\s\S]+?\\])""" +                                   // \[...\] display math
        """|(\\\([\s\S]+?\\\))""" +                                  // \(...\) inline math
        """|(?<![\\$])\$([^\s$](?:[^$\n]*?[^\s$])?)\$(?![\\$\d])""" // $...$ inline math
    )

    data class MathMatch(val range: IntRange, val expression: String, val isDisplay: Boolean)

    val mathMatches = mutableListOf<MathMatch>()
    mathPattern.findAll(content).forEach { match ->
        if (!isProtected(match.range.first)) {
            val raw = match.value
            val isDisplay = raw.startsWith("$$") || raw.startsWith("\\[")
            val expr = when {
                raw.startsWith("$$") -> raw.removeSurrounding("$$").trim()
                raw.startsWith("\\[") -> raw.removePrefix("\\[").removeSuffix("\\]").trim()
                raw.startsWith("\\(") -> raw.removePrefix("\\(").removeSuffix("\\)").trim()
                else -> raw.removeSurrounding("$").trim()
            }
            if (expr.isNotEmpty()) {
                mathMatches.add(MathMatch(match.range, expr, isDisplay))
            }
        }
    }

    // Paso 3: Ensamblar segmentos en orden
    if (mathMatches.isEmpty()) {
        return listOf(ContentSegment.Text(content))
    }

    val segments = mutableListOf<ContentSegment>()
    var cursor = 0

    mathMatches.forEach { mm ->
        if (mm.range.first > cursor) {
            val text = content.substring(cursor, mm.range.first)
            if (text.isNotBlank()) segments.add(ContentSegment.Text(text))
        }
        segments.add(ContentSegment.Math(mm.expression, mm.isDisplay))
        cursor = mm.range.last + 1
    }

    if (cursor < content.length) {
        val remaining = content.substring(cursor)
        if (remaining.isNotBlank()) segments.add(ContentSegment.Text(remaining))
    }

    return segments.ifEmpty { listOf(ContentSegment.Text(content)) }
}

// ─── Renderizador de LaTeX (Android View nativo con fallback) ────────────────

@Composable
private fun LatexFormula(
    formula: String,
    fontSize: TextUnit,
    isDisplay: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSize.toPx() }
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier.horizontalScroll(scrollState)
    ) {
        AndroidView(
            factory = { LatexFormulaView(context) },
            update = {
                it.fontSizePx = fontSizePx
                it.isDisplayMath = isDisplay
                it.formula = formula
            }
        )
    }
}

private class LatexFormulaView(context: Context) : View(context) {
    private val typeface = StixTwoMath.load(context)
    private val engine = AndroidLatexEngine(typeface)
    private val renderer = CanvasMathRenderer(typeface, AndroidColor.rgb(212, 212, 212))
    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(212, 212, 212)
        typeface = Typeface.MONOSPACE
    }
    private var mathLayout: MathLayout? = null
    private var currentFormula = ""
    private var currentFontSizePx = 0f
    var isDisplayMath = false

    var fontSizePx: Float
        get() = currentFontSizePx
        set(value) {
            if (currentFontSizePx != value) {
                currentFontSizePx = value
                fallbackPaint.textSize = value
                relayout()
            }
        }

    var formula: String
        get() = currentFormula
        set(value) {
            if (currentFormula != value) {
                currentFormula = value
                relayout()
            }
        }

    private fun relayout() {
        if (currentFormula.isNotEmpty() && currentFontSizePx > 0f) {
            mathLayout = runCatching {
                engine.layout(currentFormula, currentFontSizePx)
            }.getOrNull()
        } else {
            mathLayout = null
        }
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val layout = mathLayout
        val horizontalPadding = if (isDisplayMath) 16.dpToPx() else 8.dpToPx()
        val desiredWidth = if (layout != null) {
            layout.width.toInt() + horizontalPadding * 2
        } else {
            fallbackPaint.measureText(currentFormula).toInt() + horizontalPadding * 2
        }
        val desiredHeight = if (layout != null) {
            layout.height.toInt() + 16.dpToPx()
        } else {
            (currentFontSizePx * 1.5f).toInt() + 16.dpToPx()
        }
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val layout = mathLayout
        val hPadding = if (isDisplayMath) 16.dpToPx().toFloat() else 8.dpToPx().toFloat()
        val vPadding = 8.dpToPx().toFloat()

        if (layout != null) {
            val availableWidth = width.toFloat()
            val formulaWidth = layout.width
            val startX = if (isDisplayMath && availableWidth > formulaWidth + hPadding * 2) {
                ((availableWidth - formulaWidth) / 2f).coerceAtLeast(hPadding)
            } else {
                hPadding
            }
            renderer.draw(canvas, layout, startX, vPadding)
        } else if (currentFormula.isNotEmpty()) {
            canvas.drawText(currentFormula, hPadding, vPadding + currentFontSizePx, fallbackPaint)
        }
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()
}
