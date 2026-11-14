package com.opxl.sleepslide.presentation.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the widget layout off-screen, for each state, size and theme, to PNGs under the app's
 * external files dir (`widget-render/`). Pull them with
 * `adb pull /sdcard/Android/data/com.opxl.sleepslide.debug/files/widget-render` (picker previews: `.../widget-preview`).
 *
 * Uses Glance's own composition ([GlanceRemoteViews]) and a real RemoteViews inflate, so it shows
 * what a launcher would draw. Limitation: drawn through a software canvas, so the rounded-corner
 * outline clip is not applied (corners look square here).
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class WidgetRenderTest {

    private data class Case(val name: String, val size: DpSize, val model: WidgetModel)

    private val pinned3 = listOf(PinnedChip(1, "Rain Meditation"), PinnedChip(2, "Ocean Hearth"), PinnedChip(3, "Forest Night"))

    private val ready = WidgetModel(playing = false, hasMix = true, mixName = "Rain Meditation", timerLabel = "1h", pinned = pinned3)
    private val playing = ready.copy(playing = true)
    private val empty = WidgetModel(playing = false, hasMix = false, mixName = null, timerLabel = "30m", pinned = emptyList())

    private val cases = listOf(
        // minimum 2x1 and a more typical launcher cell
        Case("compact_min_ready", DpSize(110.dp, 40.dp), ready),
        Case("compact_ready", DpSize(160.dp, 72.dp), ready),
        Case("compact_playing", DpSize(160.dp, 72.dp), playing),
        Case("compact_empty", DpSize(160.dp, 72.dp), empty),
        Case("compact_no_timer", DpSize(160.dp, 72.dp), ready.copy(timerLabel = null)),
        Case("compact_unsaved_mix", DpSize(160.dp, 72.dp), ready.copy(mixName = null)),
        Case("compact_long_name", DpSize(160.dp, 72.dp), ready.copy(mixName = "A very long preset name that cannot fit")),
        // wide 4x2
        Case("wide_ready", DpSize(280.dp, 120.dp), ready),
        Case("wide_one_pinned", DpSize(280.dp, 120.dp), ready.copy(pinned = pinned3.take(1))),
        Case("wide_playing", DpSize(280.dp, 120.dp), playing),
        Case("wide_empty", DpSize(280.dp, 120.dp), empty),
        Case("wide_long_chip_names", DpSize(250.dp, 110.dp),
            ready.copy(pinned = listOf(PinnedChip(1, "Deep ocean vent and fire"), PinnedChip(2, "Thunderstorm over the lake"), PinnedChip(3, "Quiet forest night")))),
    )

    @Test
    fun renderAll() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val ctx = instr.targetContext
        val out = File(ctx.getExternalFilesDir(null), "widget-render").apply { deleteRecursively(); mkdirs() }

        var written = 0
        for (case in cases) {
            val rv = runBlocking {
                GlanceRemoteViews().compose(ctx, case.size) { WidgetContent(case.model) }.remoteViews
            }
            for (night in listOf(false, true)) {
                val themed = themedContext(ctx, night)
                lateinit var bitmap: Bitmap
                instr.runOnMainSync { bitmap = draw(themed, rv, case.size, night) }
                FileOutputStream(File(out, "${case.name}_${if (night) "dark" else "light"}.png")).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                written++
            }
        }
        assertTrue("expected ${cases.size * 2} images, wrote $written", written == cases.size * 2)
    }

    /** The picker preview (res/layout/widget_preview.xml) in both themes, at a typical compact size. */
    @Test
    fun renderPickerPreview() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val ctx = instr.targetContext
        // Own folder: renderAll() wipes widget-render/ and would delete these.
        val out = File(ctx.getExternalFilesDir(null), "widget-preview").apply { deleteRecursively(); mkdirs() }
        for (night in listOf(false, true)) {
            val themed = ContextThemeWrapper(themedContext(ctx, night), android.R.style.Theme_DeviceDefault_DayNight)
            lateinit var bitmap: Bitmap
            instr.runOnMainSync {
                val density = themed.resources.displayMetrics.density
                val w = (180 * density).toInt()
                val h = (72 * density).toInt()
                val margin = (12 * density).toInt()
                val parent = FrameLayout(themed)
                val view = LayoutInflater.from(themed).inflate(com.opxl.sleepslide.R.layout.widget_preview, parent, false)
                parent.addView(view, FrameLayout.LayoutParams(w, h))
                parent.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                parent.layout(0, 0, w, h)
                bitmap = Bitmap.createBitmap(w + 2 * margin, h + 2 * margin, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(if (night) 0xFF3A4A5A.toInt() else 0xFF9FB4C8.toInt())
                canvas.translate(margin.toFloat(), margin.toFloat())
                parent.draw(canvas)
            }
            FileOutputStream(File(out, "picker_preview_${if (night) "dark" else "light"}.png")).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private fun themedContext(base: Context, night: Boolean): Context {
        val config = Configuration(base.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            (if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        return base.createConfigurationContext(config)
    }

    private fun draw(ctx: Context, rv: android.widget.RemoteViews, size: DpSize, night: Boolean): Bitmap {
        val density = ctx.resources.displayMetrics.density
        val w = (size.width.value * density).toInt()
        val h = (size.height.value * density).toInt()
        val margin = (12 * density).toInt()

        val parent = FrameLayout(ctx)
        val view = rv.apply(ctx, parent)
        parent.addView(view, FrameLayout.LayoutParams(w, h))
        parent.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY),
        )
        parent.layout(0, 0, w, h)

        // Neutral backdrop so the widget's own surface is visible, like on a wallpaper.
        val bitmap = Bitmap.createBitmap(w + 2 * margin, h + 2 * margin, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(if (night) 0xFF3A4A5A.toInt() else 0xFF9FB4C8.toInt())
        canvas.translate(margin.toFloat(), margin.toFloat())
        parent.draw(canvas)
        return bitmap
    }
}
