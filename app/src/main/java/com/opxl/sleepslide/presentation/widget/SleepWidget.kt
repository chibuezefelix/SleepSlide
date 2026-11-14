package com.opxl.sleepslide.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.opxl.sleepslide.MainActivity
import com.opxl.sleepslide.R
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.service.QuickStartService
import com.opxl.sleepslide.ui.theme.DarkSurface
import com.opxl.sleepslide.ui.theme.DarkSurfaceVariant
import com.opxl.sleepslide.ui.theme.DarkText
import com.opxl.sleepslide.ui.theme.DarkTextSecondary
import com.opxl.sleepslide.ui.theme.LightPalette
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first

// Glance renders the layout for the largest declared size that fits the placed widget, so every
// breakpoint WidgetContent branches on must be declared here (narrow < 140 dp wide, roomy >= 52 dp
// tall, wide >= 220 x 100 dp). With only the two extremes, a widget between them silently got the
// smallest layout.
private val SIZE_MIN        = DpSize(110.dp, 40.dp)
private val SIZE_NARROW_TALL = DpSize(110.dp, 52.dp)
private val SIZE_ROOMY_SHORT = DpSize(140.dp, 40.dp)
private val SIZE_ROOMY      = DpSize(140.dp, 52.dp)
private val SIZE_WIDE       = DpSize(220.dp, 100.dp)

/** Same palette as the app (light + dark), expressed as Glance day/night colours. */
private object WidgetColors {
    val surface  = ColorProvider(day = LightPalette.White,      night = DarkSurface)
    val ink      = ColorProvider(day = LightPalette.Charcoal,   night = DarkText)
    val muted    = ColorProvider(day = LightPalette.MutedGray,  night = DarkTextSecondary)
    // WarmWhite, not SurfaceMuted: the lighter fill all but vanishes on the white card.
    val chip     = ColorProvider(day = LightPalette.WarmWhite, night = DarkSurfaceVariant)
    val onInk    = ColorProvider(day = LightPalette.White,      night = LightPalette.Charcoal)
    val disc     = ColorProvider(day = LightPalette.Charcoal,   night = DarkText)
}

/**
 * Home-screen widget. Compact (about 2x1): one big tap target, "Sleep now" or, while playing,
 * "Playing" with a stop tap. Wide (about 4x2): the same header plus up to three pinned presets
 * as one-tap chips. Empty state invites a first pick and opens the app.
 *
 * Draws [WidgetModel]; taps go through [QuickStartService] like the reminder button and the
 * Quick Settings tile. Redrawn by [WidgetUpdater] when playback, presets or settings change.
 */
class SleepWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SIZE_MIN, SIZE_NARROW_TALL, SIZE_ROOMY_SHORT, SIZE_ROOMY, SIZE_WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val live = modelFlow(entry)
        val initial = live.first()
        provideContent {
            // Glance keeps a session (and this composition) alive for a short while after a draw, and
            // an update during that time only recomposes it; provideGlance is NOT run again. A model
            // computed once here would go stale (a stop tap kept showing "Playing"), so the content
            // collects a live flow instead. WidgetUpdater covers the case where no session is alive.
            val model by live.collectAsState(initial)
            WidgetContent(model)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun modelFlow(entry: WidgetEntryPoint): Flow<WidgetModel> {
        val serializer = entry.mixSerializer()
        val audio = entry.audioServiceHolder().service
            .flatMapLatest { it?.audioState ?: flowOf(null) }
        return combine(
            audio,
            entry.userPreferences().observe(),
            entry.presets().observeAll(),
        ) { a, prefs, presets ->
            val sounding = a?.playbackStatus.let {
                it == Domain.PlaybackStatus.PLAYING || it == Domain.PlaybackStatus.FADING_IN ||
                    it == Domain.PlaybackStatus.LOADING
            }
            WidgetModel.build(
                prefs          = prefs,
                presets        = presets,
                playing        = sounding,
                activePresetId = a?.activePresetId,
                now            = System.currentTimeMillis(),
                deserializeMix = { runCatching { serializer.deserialize(it) }.getOrNull() },
            )
        }.distinctUntilChanged()
    }
}

/** Internal so the layout can be rendered off-screen in an instrumented test. */
@Composable
internal fun WidgetContent(model: WidgetModel) {
    val size = LocalSize.current
    val wide = size.height >= 100.dp && size.width >= 220.dp && model.pinned.isNotEmpty() && !model.playing
    val roomy = size.height >= 52.dp
    val narrow = size.width < 140.dp
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.surface)
            .cornerRadius(22.dp)
            .padding(if (roomy && !narrow) 12.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Header(model, roomy, narrow, context, modifier = if (wide) GlanceModifier.fillMaxWidth() else GlanceModifier.fillMaxSize())
        if (wide) {
            Spacer(GlanceModifier.size(10.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                model.pinned.forEachIndexed { index, chip ->
                    if (index > 0) Spacer(GlanceModifier.width(6.dp))
                    PresetChip(chip, modifier = GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

@Composable
private fun Header(model: WidgetModel, roomy: Boolean, narrow: Boolean, context: Context, modifier: GlanceModifier) {
    val title: String
    val subtitle: String
    val icon: Int
    val action: androidx.glance.action.Action
    when {
        model.playing -> {
            title    = context.getString(R.string.tile_playing)
            subtitle = model.mixName ?: context.getString(R.string.widget_your_mix)
            icon     = R.drawable.ic_stop
            action   = actionRunCallback<StopAction>()
        }
        !model.hasMix -> {
            title    = context.getString(R.string.widget_pick_first)
            subtitle = context.getString(R.string.widget_open_app)
            icon     = R.drawable.ic_moon
            action   = actionStartActivity<MainActivity>()
        }
        else -> {
            title    = context.getString(R.string.tile_label)
            // Timer first: when the line is too long it is the mix name that gets cut.
            subtitle = listOfNotNull(model.timerLabel, model.mixName ?: context.getString(R.string.widget_last_mix))
                .joinToString(" · ")
            icon     = R.drawable.ic_moon
            action   = actionRunCallback<SleepNowAction>()
        }
    }

    Row(
        modifier = modifier.clickable(action),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val disc = if (narrow) 26.dp else if (roomy) 40.dp else 28.dp
        Box(modifier = GlanceModifier.size(disc), contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_disc),
                contentDescription = null,
                colorFilter = ColorFilter.tint(WidgetColors.disc),
                modifier = GlanceModifier.fillMaxSize(),
            )
            Image(
                provider = ImageProvider(icon),
                contentDescription = title,
                colorFilter = ColorFilter.tint(WidgetColors.onInk),
                modifier = GlanceModifier.size(if (narrow) 14.dp else if (roomy) 22.dp else 16.dp),
            )
        }
        Spacer(GlanceModifier.width(if (narrow) 6.dp else 10.dp))
        Column {
            Text(
                text = title,
                maxLines = 1,
                style = TextStyle(
                    color = WidgetColors.ink,
                    fontSize = if (narrow) 13.sp else 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            if (roomy) {
                Text(
                    text = subtitle,
                    maxLines = 1,
                    style = TextStyle(color = WidgetColors.muted, fontSize = 12.sp),
                )
            }
        }
    }
}

@Composable
private fun PresetChip(chip: PinnedChip, modifier: GlanceModifier) {
    Box(
        modifier = modifier
            .background(WidgetColors.chip)
            .cornerRadius(16.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable(actionRunCallback<PresetAction>(actionParametersOf(PresetIdKey to chip.id))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = chip.name,
            maxLines = 2,
            style = TextStyle(color = WidgetColors.ink, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
    }
}

internal val PresetIdKey = ActionParameters.Key<Long>("preset_id")

private suspend fun runQuickStart(context: Context, options: QuickStartService.Options) {
    val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
    val outcome = entry.quickStart().start(options)
    when (outcome) {
        is QuickStartService.Outcome.Started,
        is QuickStartService.Outcome.AlreadyPlaying -> Unit
        // A widget has no room for a message; explain through the same silent notice the reminder uses.
        else -> entry.windDown().onQuickStartResult(outcome)
    }
    SleepWidget().updateAll(context)
}

class SleepNowAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        runQuickStart(context, QuickStartService.Options(maxAgeMs = null, applyTimer = true))
    }
}

class PresetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[PresetIdKey] ?: return
        runQuickStart(context, QuickStartService.Options(applyTimer = true, presetId = id))
    }
}

class StopAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        entry.quickStart().stop()
        SleepWidget().updateAll(context)
    }
}
