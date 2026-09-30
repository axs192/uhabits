package org.isoron.uhabits.core.models

import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Daily numerical habit, fourteen days of history with 10 logged every day.
 * The schedule asks for 10 during the first week and 20 during the second.
 */
class ScheduledTargetHabitTest : BaseUnitTest() {
    private lateinit var habit: Habit
    private lateinit var today: LocalDate
    private lateinit var schedule: TargetSchedule

    @BeforeTest
    override fun setUp() {
        super.setUp()
        today = getToday()
        habit = fixtures.createEmptyNumericalHabit(NumericalHabitType.AT_LEAST)
        habit.frequency = Frequency.DAILY
        habit.targetValue = 10.0
        schedule = TargetSchedule(
            start = today.minus(13),
            stageLength = 7,
            values = listOf(10.0, 20.0)
        )
        for (i in 0..13) habit.originalEntries.add(Entry(today.minus(i), 10_000))
    }

    @Test
    fun testTargetValueOn_withoutSchedule() {
        assertNull(habit.targetSchedule)
        assertEquals(10.0, habit.targetValueOn(today))
        assertEquals(10.0, habit.targetValueOn(today.minus(100)))
    }

    @Test
    fun testTargetValueOn_withSchedule() {
        habit.targetValue = 3.0
        habit.targetSchedule = schedule
        assertEquals(3.0, habit.targetValueOn(today.minus(14)))
        assertEquals(10.0, habit.targetValueOn(today.minus(13)))
        assertEquals(10.0, habit.targetValueOn(today.minus(7)))
        assertEquals(20.0, habit.targetValueOn(today.minus(6)))
        assertEquals(20.0, habit.targetValueOn(today))
        assertEquals(20.0, habit.targetValueOn(today.plus(30)))
    }

    @Test
    fun testScore_withoutSchedule_isUnchanged() {
        habit.recompute()
        assertEquals(expectedScore(List(14) { 1.0 }), habit.scores[today].value, 1e-9)
    }

    @Test
    fun testScore_usesTargetOfEachDate() {
        habit.targetSchedule = schedule
        habit.recompute()
        val percentages = List(7) { 1.0 } + List(7) { 0.5 }
        assertEquals(expectedScore(percentages), habit.scores[today].value, 1e-9)
        assertEquals(
            expectedScore(List(7) { 1.0 }),
            habit.scores[today.minus(7)].value,
            1e-9
        )
    }

    @Test
    fun testStreaks_useTargetOfEachDate() {
        habit.targetSchedule = schedule
        habit.recompute()
        val best = habit.streaks.getBest(5)
        assertEquals(1, best.size)
        assertEquals(today.minus(13), best[0].start)
        assertEquals(today.minus(7), best[0].end)
    }

    @Test
    fun testIsCompletedToday_usesTodaysTarget() {
        habit.targetSchedule = schedule
        habit.recompute()
        assertFalse(habit.isCompletedToday())

        habit.originalEntries.add(Entry(today, 20_000))
        habit.recompute()
        assertTrue(habit.isCompletedToday())
    }

    @Test
    fun testCopyFrom_copiesSchedule() {
        habit.targetSchedule = schedule
        val other = modelFactory.buildHabit()
        other.copyFrom(habit)
        assertEquals(schedule, other.targetSchedule)
    }

    @Test
    fun testEquals_considersSchedule() {
        val other = modelFactory.buildHabit()
        other.copyFrom(habit)
        assertEquals(habit, other)
        assertEquals(habit.hashCode(), other.hashCode())

        other.targetSchedule = schedule
        assertNotEquals(habit, other)
    }

    private fun expectedScore(percentages: List<Double>): Double {
        var score = 0.0
        for (p in percentages) score = Score.compute(1.0, score, p)
        return score
    }
}
