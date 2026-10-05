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
 * Shows a goal's current stage, the current month, its overall total and
 * the days used. Once the goal has
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

        // While the goal is in progress, the current stage and month come first. Then
        // what's done overall (highlighted in the habit colour) and the days used,
        // whose remainder is the number of days left
        val color = theme.color(habit.color).toInt()
        val labels = mutableListOf<String>()
        val targets = mutableListOf<Double>()
        val values = mutableListOf<Double>()
        val colors = mutableListOf<Int>()
        fun addRow(label: String, target: Double, value: Double, rowColor: Int = color) {
            labels.add(label)
            targets.add(target)
            values.add(value)
            colors.add(rowColor)
        }
        val stageTarget = state.currentStageTarget
        val stageActual = state.currentStageActual
        if (stageTarget != null && stageActual != null) {
            val stageLabel = if (state.stageLength == 7) R.string.goal_widget_this_week else R.string.goal_widget_this_stage
            addRow(res.getString(stageLabel), stageTarget, stageActual)
        }
        val monthTarget = state.monthTarget
        val monthActual = state.monthActual
        if (monthTarget != null && monthActual != null) {
            addRow(res.getString(R.string.goal_widget_this_month), monthTarget, monthActual)
        }
        addRow(res.getString(R.string.goal_total), state.totalTarget, state.totalActual)
        addRow(
            res.getString(R.string.goal_widget_days),
            state.goalLengthDays.toDouble(),
            state.daysUsed.toDouble(),
            theme.mediumContrastTextColor.toInt()
        )
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
