package org.isoron.uhabits.core.models

import org.isoron.platform.time.LocalDate
import kotlin.math.max
import kotlin.math.min

enum class GoalStatus {
    NOT_STARTED, IN_PROGRESS, ACHIEVED, NOT_ACHIEVED
}

/**
 * One stage of a goal. [actual] adds up the entries logged within the
 * stage. A stage counts as met once the amount is reached; for "at most"
 * goals, only once the stage is over.
 */
data class GoalStage(
    val index: Int,
    val start: LocalDate,
    val end: LocalDate,
    val target: Double,
    val actual: Double,
    val isMet: Boolean
)

/**
 * Where a goal stands on a given day. The goal is achieved when, once it
 * has ended, the total logged over all stages reaches the total of the
 * stage targets (or stays within it, for "at most" goals).
 */
data class GoalProgress(
    val stages: List<GoalStage>,
    val endDate: LocalDate,
    val currentStageIndex: Int?,
    val totalTarget: Double,
    val totalActual: Double,
    val stagesMet: Int,
    val daysLeft: Int,
    val status: GoalStatus,
    val fraction: Double
) {
    companion object {
        /**
         * Returns the progress of [habit] as of [today], or null if the habit
         * has no target schedule.
         */
        fun compute(habit: Habit, today: LocalDate): GoalProgress? {
            val schedule = habit.targetSchedule ?: return null
            val isAtMost = habit.targetType == NumericalHabitType.AT_MOST
            val endDate = schedule.endDate

            val stages = schedule.values.mapIndexed { index, target ->
                val start = schedule.stageStart(index)
                val end = start.plus(schedule.stageLength - 1)
                val actual = habit.computedEntries
                    .getByInterval(start, end)
                    .sumOf { if (it.value == Entry.SKIP) 0 else max(0, it.value) } / 1000.0
                val isOver = end.isOlderThan(today)
                val isMet = if (isAtMost) isOver && actual <= target else actual >= target
                GoalStage(index, start, end, target, actual, isMet)
            }

            val totalTarget = stages.sumOf { it.target }
            val totalActual = stages.sumOf { it.actual }
            val status = when {
                today.isOlderThan(schedule.start) -> GoalStatus.NOT_STARTED
                !today.isNewerThan(endDate) -> GoalStatus.IN_PROGRESS
                isAtMost && totalActual <= totalTarget -> GoalStatus.ACHIEVED
                !isAtMost && totalActual >= totalTarget -> GoalStatus.ACHIEVED
                else -> GoalStatus.NOT_ACHIEVED
            }
            val daysLeft = when (status) {
                GoalStatus.NOT_STARTED -> schedule.start.daysUntil(endDate) + 1
                GoalStatus.IN_PROGRESS -> today.daysUntil(endDate) + 1
                else -> 0
            }
            val fraction = when {
                isAtMost && totalActual <= totalTarget -> if (status == GoalStatus.NOT_STARTED) 0.0 else 1.0
                isAtMost -> totalTarget / totalActual
                totalTarget <= 0 -> 1.0
                else -> min(1.0, totalActual / totalTarget)
            }

            return GoalProgress(
                stages = stages,
                endDate = endDate,
                currentStageIndex = if (status == GoalStatus.IN_PROGRESS) schedule.stageIndexOn(today) else null,
                totalTarget = totalTarget,
                totalActual = totalActual,
                stagesMet = stages.count { it.isMet },
                daysLeft = daysLeft,
                status = status,
                fraction = fraction
            )
        }
    }
}
