package com.opxl.sleepslide.presentation.components

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * User-facing text that a ViewModel can emit without holding a [Context].
 * Resolved to a localised string at the UI edge via [asString].
 */
sealed interface UiText {

    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** User-entered or catalogue data (a preset name, an unknown sound). Never an exception message. */
    data class Verbatim(val value: String) : UiText

    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = listOf(count)) : UiText

    fun asString(context: Context): String = when (this) {
        is Res      -> context.getString(id, *args.resolve(context))
        is Verbatim -> value
        is Plural   -> context.resources.getQuantityString(id, count, *args.resolve(context))
    }

    @Composable
    fun asString(): String = asString(LocalContext.current)

    companion object {
        operator fun invoke(@StringRes id: Int, vararg args: Any): UiText = Res(id, args.toList())
    }
}

/** Nested [UiText] args (e.g. a formatted duration inside a sentence) resolve first. */
private fun List<Any>.resolve(context: Context): Array<Any> =
    map { if (it is UiText) it.asString(context) else it }.toTypedArray()
