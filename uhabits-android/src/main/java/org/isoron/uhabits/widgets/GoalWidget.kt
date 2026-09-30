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
        val state = GoalCardPresenter.buildState(habit, WidgetTheme())
        val res = context.resources
        widgetView.setTitle(
            when (state.status) {
                GoalStatus.IN_PROGRESS -> res.getString(R.string.goal_widget_title_days_left, habit.name, state.daysLeft)
                GoalStatus.ACHIEVED -> res.getString(R.string.goal_widget_title_status, habit.name, res.getString(R.string.goal_status_achieved))
                GoalStatus.NOT_ACHIEVED -> res.getString(R.string.goal_widget_title_status, habit.name, res.getString(R.string.goal_status_not_achieved))
                GoalStatus.NOT_STARTED -> habit.name
            }
        )

        val labels = mutableListOf<String>()
        val values = mutableListOf<Double>()
        val targets = mutableListOf<Double>()
        if (state.stageNumber > 0) {
            val labelRes = if (state.stageLength == 7) R.string.schedule_week_n else R.string.schedule_stage_n
            labels.add(res.getString(labelRes, state.stageNumber))
            values.add(state.stageActuals[state.stageNumber - 1])
            targets.add(state.stageTargets[state.stageNumber - 1])
        }
        labels.add(res.getString(R.string.goal_total))
        values.add(state.totalActual)
        targets.add(state.totalTarget)

        val chart = widgetView.dataView as TargetChart
        chart.setColor(WidgetTheme().color(habit.color).toInt())
        chart.setTargets(targets)
        chart.setLabels(labels)
        chart.setValues(values)
    }

    override fun buildView(): View {
        return GraphWidgetView(context, TargetChart(context)).apply {
            setTitle(habit.name)
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
    }
}
