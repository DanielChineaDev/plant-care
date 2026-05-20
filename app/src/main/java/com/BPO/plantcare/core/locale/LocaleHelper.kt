package com.BPO.plantcare.core.locale

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Gestion sencilla del idioma de la app sin AppCompat: guardamos el codigo
 * de idioma en SharedPreferences (lectura sincrona, necesaria en
 * attachBaseContext) y envolvemos el Context con la Locale elegida.
 *
 * El idioma base de la app es espanol (recursos en values/). El ingles vive
 * en values-en/. Si el usuario no ha elegido nada, usamos espanol.
 */
object LocaleHelper {
    private const val PREFS = "plantcare_locale"
    private const val KEY_LANG = "lang"
    const val DEFAULT_LANG = "es"

    /** Idiomas soportados (codigo ISO -> etiqueta nativa). */
    val SUPPORTED: List<Pair<String, String>> = listOf(
        "es" to "Español",
        "en" to "English",
    )

    fun getLanguage(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, DEFAULT_LANG) ?: DEFAULT_LANG

    fun setLanguage(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, lang).apply()
    }

    /** Devuelve un Context configurado con la Locale guardada. */
    fun wrap(context: Context): Context {
        val locale = Locale(getLanguage(context))
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
