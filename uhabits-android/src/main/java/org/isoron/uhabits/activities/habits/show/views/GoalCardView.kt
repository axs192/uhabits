package org.isoron.uhabits.activities.habits.show.views

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.list.views.toShortString
import org.isoron.uhabits.core.models.GoalStatus
import org.isoron.uhabits.core.ui.screens.habits.show.views.GoalCardState
import org.isoron.uhabits.databinding.ShowHabitGoalBinding
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

class GoalCardView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private val binding = ShowHabitGoalBinding.inflate(LayoutInflater.from(context), this)

    fun setState(state: GoalCardState) {
        if (!state.isVisible) {
            visibility = GONE
            return
        }
        visibility = VISIBLE
        val androidColor = state.theme.color(state.color).toInt()
        binding.title.setTextColor(androidColor)
        binding.headerText.text = headerText(resources, state)

        val labelRes = if (state.stageLength == 7) R.string.schedule_week_n else R.string.schedule_stage_n
        val labels = state.stageTargets.indices.map { resources.getString(labelRes, it + 1) } +
            resources.getString(R.string.goal_total)
        binding.stagesChart.setColor(androidColor)
        binding.stagesChart.setLabels(labels)
        binding.stagesChart.setTargets(state.stageTargets + state.totalTarget)
        binding.stagesChart.setValues(state.stageActuals + state.totalActual)
        binding.stagesChart.requestLayout()
        postInvalidate()
    }

    companion object {
        fun headerText(resources: Resources, state: GoalCardState): String {
            val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            return when (state.status) {
                GoalStatus.NOT_STARTED -> {
                    val start = state.endDate!!.minus(state.stageCount * state.stageLength - 1)
                    resources.getString(R.string.goal_header_not_started, dateFormat.format(Date(start.unixTime)))
                }
                GoalStatus.IN_PROGRESS -> resources.getString(
                    if (state.stageLength == 7) R.string.goal_header_in_progress_week else R.string.goal_header_in_progress_stage,
                    state.stageNumber,
                    state.stageCount,
                    state.daysLeft
                )
                GoalStatus.ACHIEVED, GoalStatus.NOT_ACHIEVED -> resources.getString(
                    R.string.goal_header_ended,
                    resources.getString(
                        if (state.status == GoalStatus.ACHIEVED) R.string.goal_status_achieved else R.string.goal_status_not_achieved
                    ),
                    state.totalActual.toShortString(),
                    "${state.totalTarget.toShortString()} ${state.unit}".trim(),
                    state.stagesMet,
                    state.stageCount
                )
            }
        }
    }
}
