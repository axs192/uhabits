package org.isoron.uhabits.widgets

import android.app.PendingIntent
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.TargetChart
import org.isoron.uhabits.activities.habits.list.views.toShortString
import org.isoron.uhabits.core.models.GoalStatus
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.ui.screens.habits.show.views.GoalCardPresenter
import org.isoron.uhabits.core.ui.views.WidgetTheme
import org.isoron.uhabits.widgets.views.GraphWidgetView

/**
 * Shows a goal's current stage, its overall total and the days used, with
 * the amount needed per day to meet the stage in the title. Once the goal has
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
                else -> state.dailyNeed?.let {
                    res.getString(R.string.goal_widget_title_daily_need, habit.name, it.toShortString(), habit.unit)
                } ?: habit.name
            }
        )

        // While a stage is in progress, its own progress comes first. Then what's done
        // overall (highlighted in the habit colour) and the days used, whose remainder
        // is the number of days left
        val color = theme.color(habit.color).toInt()
        val labels = mutableListOf(res.getString(R.string.goal_total), res.getString(R.string.goal_widget_days))
        val targets = mutableListOf(state.totalTarget, state.goalLengthDays.toDouble())
        val values = mutableListOf(state.totalActual, state.daysUsed.toDouble())
        val colors = mutableListOf(color, theme.mediumContrastTextColor.toInt())
        val stageTarget = state.currentStageTarget
        val stageActual = state.currentStageActual
        if (stageTarget != null && stageActual != null) {
            val stageLabel = if (state.stageLength == 7) R.string.goal_widget_this_week else R.string.goal_widget_this_stage
            labels.add(0, res.getString(stageLabel))
            targets.add(0, stageTarget)
            values.add(0, stageActual)
            colors.add(0, color)
        }
        val chart = widgetView.dataView as TargetChart
        chart.setColor(color)
        chart.setBarColors(colors)
        chart.setAlwaysShowCompleted(true)
        chart.setLabels(labels)
        chart.setTargets(targets)
        chart.setValues(values)
    }

    override fun buildView(): View {
        return GraphWidgetView(context, TargetChart(context)).apply {
            setTitle(habit.name)
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
    }
}
