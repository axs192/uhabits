package org.isoron.uhabits.widgets

import android.app.PendingIntent
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.TargetChart
import org.isoron.uhabits.core.models.GoalStatus
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.ui.screens.habits.show.views.GoalCardPresenter
import org.isoron.uhabits.core.ui.views.WidgetTheme
import org.isoron.uhabits.widgets.views.GraphWidgetView

/**
 * Shows a goal's current stage and its overall total. Once the goal has
 * ended, it shows the result instead.
 */
class GoalWidget(
    context: Context,
    id: Int,
    private val habit: Habit,
    stacked: Boolean = false
) : BaseWidget(context, id, stacked) {
    override val defaultHeight: Int = 200
    override val defaultWidth: Int = 200

    override fun getOnClickPendingIntent(context: Context): PendingIntent =
        pendingIntentFactory.showHabit(habit)

    override fun refreshData(view: View) {
        val widgetView = view as GraphWidgetView
        widgetView.setBackgroundAlpha(preferedBackgroundAlpha)
        if (preferedBackgroundAlpha >= 255) widgetView.setShadowAlpha(0x4f)
        val theme = WidgetTheme()
        val state = GoalCardPresenter.buildState(habit, theme)
        val res = context.resources
        widgetView.setTitle(
            when (state.status) {
                GoalStatus.ACHIEVED -> res.getString(R.string.goal_widget_title_achieved, habit.name)
                GoalStatus.NOT_ACHIEVED -> res.getString(R.string.goal_widget_title_not_achieved, habit.name)
                else -> habit.name
            }
        )

        // Two rows: what's done (highlighted in the habit colour) and the days used,
        // whose remainder is the number of days left
        val chart = widgetView.dataView as TargetChart
        chart.setColor(theme.color(habit.color).toInt())
        chart.setBarColors(listOf(theme.color(habit.color).toInt(), theme.mediumContrastTextColor.toInt()))
        chart.setAlwaysShowCompleted(true)
        chart.setLabels(listOf(res.getString(R.string.goal_total), res.getString(R.string.goal_widget_days)))
        chart.setTargets(listOf(state.totalTarget, state.goalLengthDays.toDouble()))
        chart.setValues(listOf(state.totalActual, state.daysUsed.toDouble()))
    }

    override fun buildView(): View {
        return GraphWidgetView(context, TargetChart(context)).apply {
            setTitle(habit.name)
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
    }
}
