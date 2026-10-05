package dj.nomade.velox_restaurant.core.locale

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.util.Locale

/**
 * Manages the app's user-selected display language, independent of the device locale.
 *
 * Persistence uses plain SharedPreferences (not the DataStore in AppModule) because the
 * selected language must be readable synchronously from Activity/Application.attachBaseContext,
 * which runs before coroutines/Hilt are safely usable.
 */
object AppLocale {
    const val FRENCH = "fr"
    const val ENGLISH = "en"
    const val ARABIC = "ar"

    val SUPPORTED = listOf(FRENCH, ENGLISH, ARABIC)
    private const val DEFAULT_LANGUAGE = FRENCH

    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "app_language"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLanguage(context: Context): String {
        val stored = prefs(context).getString(KEY_LANGUAGE, DEFAULT_LANGUAGE)
        return if (stored in SUPPORTED) stored!! else DEFAULT_LANGUAGE
    }

    fun setLanguage(context: Context, languageCode: String) {
        require(languageCode in SUPPORTED) { "Unsupported language: $languageCode" }
        prefs(context).edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    /** Wraps [context] with a Configuration forcing the persisted app language. */
    fun wrapContext(context: Context): Context {
        val locale = buildLocale(getLanguage(context))
        Locale.setDefault(locale)
        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(android.os.LocaleList(locale))
        }
        return context.createConfigurationContext(configuration)
    }

    /**
     * Arabic CLDR data defaults to Eastern Arabic-Indic digits (٠-٩). Djibouti uses Western
     * digits regardless of language, so the Arabic locale explicitly forces the Latin (`latn`)
     * numbering system to keep FDJ amounts and counters readable.
     */
    private fun buildLocale(languageCode: String): Locale {
        val builder = Locale.Builder().setLanguage(languageCode)
        if (languageCode == ARABIC) builder.setUnicodeLocaleKeyword("nu", "latn")
        return builder.build()
    }
}
