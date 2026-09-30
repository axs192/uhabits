package org.isoron.uhabits.core.models

import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.LocalDate
import kotlin.math.min

/**
 * A progressive target for a numerical habit. The schedule is split into
 * stages of equal length, each with its own target. Stages advance by
 * calendar, and the target of the last stage holds indefinitely.
 */
data class TargetSchedule(
    val start: LocalDate,
    val stageLength: Int,
    val values: List<Double>
) {
    init {
        require(stageLength > 0) { "stageLength must be positive" }
        require(values.isNotEmpty()) { "schedule must have at least one stage" }
    }

    /**
     * Returns the index of the stage in force on the given date, or null if
     * the schedule has not started yet.
     */
    fun stageIndexOn(date: LocalDate): Int? {
        if (date.isOlderThan(start)) return null
        return min(start.daysUntil(date) / stageLength, values.size - 1)
    }

    /**
     * Returns the target in force on the given date, or [fallback] if the
     * schedule has not started yet.
     */
    fun valueOn(date: LocalDate, fallback: Double): Double {
        val index = stageIndexOn(date) ?: return fallback
        return values[index]
    }

    fun stageStart(index: Int): LocalDate = start.plus(index * stageLength)

    /**
     * Returns the date on which the next stage begins, together with its
     * target, or null if the last stage has already been reached.
     */
    fun nextChange(date: LocalDate): Pair<LocalDate, Double>? {
        val next = (stageIndexOn(date) ?: -1) + 1
        if (next >= values.size) return null
        return Pair(stageStart(next), values[next])
    }

    fun serializeValues(): String = values.joinToString(SEPARATOR)

    companion object {
        private const val SEPARATOR = ";"

        /**
         * Returns the start date to use for a schedule. Stages lasting a whole
         * number of weeks start on the first day of the week, so that each
         * calendar week falls within a single stage.
         */
        fun alignStart(date: LocalDate, stageLength: Int, firstWeekday: DayOfWeek): LocalDate {
            if (stageLength % 7 != 0) return date
            return date.startOfWeek(firstWeekday)
        }

        fun parseValues(text: String): List<Double> {
            return text.split(SEPARATOR).mapNotNull { it.trim().toDoubleOrNull() }
        }
    }
}
