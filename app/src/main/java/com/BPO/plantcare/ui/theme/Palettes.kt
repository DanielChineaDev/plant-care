package com.BPO.plantcare.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paletas de color seleccionables por el usuario. Cada una define la familia
 * de color "primary" (light y dark); el resto del esquema (fondos neutros,
 * superficies) se hereda del esquema base. La key se persiste en preferencias.
 */
enum class AppPalette(
    val key: String,
    val label: String,
    /** Color de muestra para el selector. */
    val swatch: Color,
    val light: PaletteColors,
    val dark: PaletteColors,
) {
    Green(
        key = "green",
        label = "Verde",
        swatch = Color(0xFF2E7D32),
        light = PaletteColors(
            primary = Color(0xFF2E7D32),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFB7E4B9),
            onPrimaryContainer = Color(0xFF002106),
        ),
        dark = PaletteColors(
            primary = Color(0xFF9CD49C),
            onPrimary = Color(0xFF003910),
            primaryContainer = Color(0xFF0F5114),
            onPrimaryContainer = Color(0xFFB7E4B9),
        ),
    ),
    Ochre(
        key = "ochre",
        label = "Ocre",
        swatch = Color(0xFFB07A2E),
        light = PaletteColors(
            primary = Color(0xFF9A6A1F),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF4DFC1),
            onPrimaryContainer = Color(0xFF2E1E00),
        ),
        dark = PaletteColors(
            primary = Color(0xFFE5C08A),
            onPrimary = Color(0xFF402D00),
            primaryContainer = Color(0xFF5C411A),
            onPrimaryContainer = Color(0xFFF4DFC1),
        ),
    ),
    Lavender(
        key = "lavender",
        label = "Lavanda",
        swatch = Color(0xFF7E57C2),
        light = PaletteColors(
            primary = Color(0xFF6A4BB0),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE6DEF7),
            onPrimaryContainer = Color(0xFF22074F),
        ),
        dark = PaletteColors(
            primary = Color(0xFFC9B6F0),
            onPrimary = Color(0xFF36215E),
            primaryContainer = Color(0xFF4A357A),
            onPrimaryContainer = Color(0xFFE6DEF7),
        ),
    ),
    Ocean(
        key = "ocean",
        label = "Océano",
        swatch = Color(0xFF0277BD),
        light = PaletteColors(
            primary = Color(0xFF02669E),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFBEE3F7),
            onPrimaryContainer = Color(0xFF001E2E),
        ),
        dark = PaletteColors(
            primary = Color(0xFF8FCDEC),
            onPrimary = Color(0xFF00344C),
            primaryContainer = Color(0xFF064E73),
            onPrimaryContainer = Color(0xFFBEE3F7),
        ),
    ),
    Rose(
        key = "rose",
        label = "Rosa",
        swatch = Color(0xFFC2185B),
        light = PaletteColors(
            primary = Color(0xFFB01453),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF7C9DC),
            onPrimaryContainer = Color(0xFF3E0020),
        ),
        dark = PaletteColors(
            primary = Color(0xFFF0A6C2),
            onPrimary = Color(0xFF5E1133),
            primaryContainer = Color(0xFF7A1340),
            onPrimaryContainer = Color(0xFFF7C9DC),
        ),
    );

    companion object {
        fun fromKey(key: String?): AppPalette = entries.firstOrNull { it.key == key } ?: Green
    }
}

/** Cuarteto de colores de la familia "primary" para una paleta. */
data class PaletteColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
)
