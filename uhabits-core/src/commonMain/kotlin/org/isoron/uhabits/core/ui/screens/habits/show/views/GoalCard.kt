package org.isoron.uhabits.core.ui.screens.habits.show.views

import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.TruncateField
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.GoalProgress
import org.isoron.uhabits.core.models.GoalStatus
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.ui.views.Theme
import kotlin.math.max

/**
 * State of the goal card. [stageNumber] counts from one and is zero when no
 * stage is in progress (before the goal starts, or after it ends).
 */
data class GoalCardState(
    val isVisible: Boolean,
    val color: PaletteColor,
    val theme: Theme,
    val status: GoalStatus = GoalStatus.NOT_STARTED,
    val stageNumber: Int = 0,
    val stageCount: Int = 0,
    val stageLength: Int = 0,
    val endDate: LocalDate? = null,
    val daysLeft: Int = 0,
    val stageTargets: List<Double> = listOf(),
    val stageActuals: List<Double> = listOf(),
    val totalTarget: Double = 0.0,
    val totalActual: Double = 0.0,
    val stagesMet: Int = 0,
    val unit: String = "",
    val monthTarget: Double? = null,
    val monthActual: Double? = null
) {
    /** Number of days from the goal's start to its end. */
    val goalLengthDays: Int
        get() = stageCount * stageLength

    /** Days of the goal already behind us; together with [daysLeft] this makes [goalLengthDays]. */
    val daysUsed: Int
        get() = goalLengthDays - daysLeft

    /** Target of the stage in progress, or null when no stage is in progress. */
    val currentStageTarget: Double?
        get() = stageTargets.getOrNull(stageNumber - 1)

    /** Amount logged in the stage in progress, or null when no stage is in progress. */
    val currentStageActual: Double?
        get() = stageActuals.getOrNull(stageNumber - 1)
}

class GoalCardPresenter {
    companion object {
        fun buildState(habit: Habit, theme: Theme): GoalCardState {
            val schedule = habit.targetSchedule
            val today = getToday()
            val progress = if (habit.isGoal) GoalProgress.compute(habit, today) else null
            if (schedule == null || progress == null) {
                return GoalCardState(isVisible = false, color = habit.color, theme = theme)
            }
            val inProgress = progress.status == GoalStatus.IN_PROGRESS
            val monthStart = today.startOfMonth()
            return GoalCardState(
                isVisible = true,
                color = habit.color,
                theme = theme,
                status = progress.status,
                stageNumber = (progress.currentStageIndex ?: -1) + 1,
                stageCount = progress.stages.size,
                stageLength = schedule.stageLength,
                endDate = progress.endDate,
                daysLeft = progress.daysLeft,
                stageTargets = progress.stages.map { it.target },
                stageActuals = progress.stages.map { it.actual },
                totalTarget = progress.totalTarget,
                totalActual = progress.totalActual,
                stagesMet = progress.stagesMet,
                unit = habit.unit,
                monthTarget = if (inProgress) habit.goalTargetOverDays(monthStart, monthStart.monthLength) else null,
                monthActual = if (inProgress) habit.goalActualBetween(monthStart, today) else null
            )
        }
    }
}

/**
 * Returns the number of days in the calendar period that starts at [start].
 */
internal fun periodLength(start: LocalDate, field: TruncateField): Int = when (field) {
    TruncateField.DAY -> 1
    TruncateField.WEEK_NUMBER -> 7
    TruncateField.MONTH -> start.monthLength
    TruncateField.QUARTER -> {
        var end = start
        repeat(3) { end = end.plus(end.monthLength) }
        start.daysUntil(end)
    }
    TruncateField.YEAR -> start.yearLength
}

/**
 * Returns what a goal asks for over [days] days beginning at [start]: each
 * day within the goal contributes its stage target divided by the stage
 * length.
 */
internal fun Habit.goalTargetOverDays(start: LocalDate, days: Int): Double {
    var total = 0.0
    for (i in 0 until days) {
        val date = start.plus(i)
        if (!isActiveOn(date)) continue
        total += targetValueOn(date) / frequency.denominator
    }
    return total
}

/**
 * Returns the amount logged from [start] to [end], counting only the days
 * within the goal.
 */
internal fun Habit.goalActualBetween(start: LocalDate, end: LocalDate): Double =
    computedEntries.getByInterval(start, end)
        .filter { isActiveOn(it.date) && it.value != Entry.SKIP }
        .sumOf { max(0, it.value) } / 1000.0
