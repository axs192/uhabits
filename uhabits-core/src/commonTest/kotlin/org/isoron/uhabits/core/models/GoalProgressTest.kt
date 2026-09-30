package org.isoron.uhabits.core.models

import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Today is Sunday 2015-01-25. Stages last seven days.
 */
class GoalProgressTest : BaseUnitTest() {
    private lateinit var today: LocalDate
    private lateinit var goal: Habit

    @BeforeTest
    override fun setUp() {
        super.setUp()
        today = getToday()
        goal = fixtures.createEmptyNumericalHabit(NumericalHabitType.AT_LEAST)
        goal.type = HabitType.GOAL
    }

    private fun plan(start: LocalDate, vararg values: Double) {
        goal.targetSchedule = TargetSchedule(start, 7, values.toList())
        goal.frequency = Frequency(1, 7)
    }

    private fun log(from: LocalDate, to: LocalDate, amount: Int) {
        var date = from
        while (!date.isNewerThan(to)) {
            goal.originalEntries.add(Entry(date, amount * 1000))
            date = date.plus(1)
        }
        goal.recompute()
    }

    private fun progress() = GoalProgress.compute(goal, today)!!

    @Test
    fun testNoScheduleMeansNoProgress() {
        assertNull(GoalProgress.compute(goal, today))
    }

    @Test
    fun testInProgress() {
        plan(today.minus(13), 10.0, 20.0, 30.0)
        log(today.minus(13), today, 2)
        val p = progress()
        assertEquals(GoalStatus.IN_PROGRESS, p.status)
        assertEquals(today.minus(13).plus(20), p.endDate)
        assertEquals(1, p.currentStageIndex)
        assertEquals(listOf(14.0, 14.0, 0.0), p.stages.map { it.actual })
        assertEquals(listOf(10.0, 20.0, 30.0), p.stages.map { it.target })
        assertEquals(listOf(true, false, false), p.stages.map { it.isMet })
        assertEquals(1, p.stagesMet)
        assertEquals(60.0, p.totalTarget)
        assertEquals(28.0, p.totalActual)
        assertEquals(8, p.daysLeft)
        assertEquals(28.0 / 60.0, p.fraction, 1e-9)
    }

    @Test
    fun testNotStarted() {
        plan(today.plus(2), 10.0, 20.0, 30.0)
        val p = progress()
        assertEquals(GoalStatus.NOT_STARTED, p.status)
        assertNull(p.currentStageIndex)
        assertEquals(21, p.daysLeft)
        assertEquals(0.0, p.totalActual)
        assertEquals(0.0, p.fraction)
    }

    @Test
    fun testEnded_achieved() {
        plan(today.minus(28), 20.0, 21.0)
        log(today.minus(28), today.minus(15), 3)
        val p = progress()
        assertEquals(GoalStatus.ACHIEVED, p.status)
        assertEquals(today.minus(15), p.endDate)
        assertNull(p.currentStageIndex)
        assertEquals(0, p.daysLeft)
        assertEquals(2, p.stagesMet)
        assertEquals(42.0, p.totalActual)
        assertEquals(1.0, p.fraction)
    }

    @Test
    fun testEnded_notAchieved() {
        plan(today.minus(28), 10.0, 40.0)
        log(today.minus(28), today.minus(15), 3)
        val p = progress()
        assertEquals(GoalStatus.NOT_ACHIEVED, p.status)
        assertEquals(1, p.stagesMet)
        assertEquals(42.0 / 50.0, p.fraction, 1e-9)
    }

    @Test
    fun testTotalDecidesEvenWhenAStageWasMissed() {
        plan(today.minus(28), 30.0, 10.0)
        log(today.minus(28), today.minus(15), 3)
        val p = progress()
        assertEquals(listOf(false, true), p.stages.map { it.isMet })
        assertEquals(GoalStatus.ACHIEVED, p.status)
    }

    @Test
    fun testEntriesOutsideTheGoalDoNotCount() {
        plan(today.minus(28), 20.0, 21.0)
        log(today.minus(40), today, 3)
        assertEquals(42.0, progress().totalActual)
    }

    @Test
    fun testSkippedDaysCountAsZero() {
        plan(today.minus(13), 10.0, 20.0, 30.0)
        log(today.minus(13), today, 2)
        goal.originalEntries.add(Entry(today, Entry.SKIP))
        goal.recompute()
        assertEquals(12.0, progress().stages[1].actual)
    }

    @Test
    fun testAtMost() {
        goal.targetType = NumericalHabitType.AT_MOST
        plan(today.minus(28), 25.0, 20.0)
        log(today.minus(28), today.minus(15), 3)
        val p = progress()
        assertEquals(listOf(true, false), p.stages.map { it.isMet })
        assertEquals(GoalStatus.ACHIEVED, p.status)
        assertEquals(1.0, p.fraction)
    }

    @Test
    fun testAtMost_stageInProgressIsNotMetYet() {
        goal.targetType = NumericalHabitType.AT_MOST
        plan(today.minus(3), 100.0)
        log(today.minus(3), today, 3)
        val p = progress()
        assertEquals(GoalStatus.IN_PROGRESS, p.status)
        assertFalse(p.stages[0].isMet)
    }

    @Test
    fun testGoalType() {
        assertEquals(HabitType.GOAL, HabitType.fromInt(2))
        assertTrue(goal.isGoal)
        assertTrue(goal.isNumerical)
        goal.type = HabitType.NUMERICAL
        assertFalse(goal.isGoal)
    }

    @Test
    fun testIsActiveOn() {
        plan(today.minus(13), 10.0, 20.0, 30.0)
        assertFalse(goal.isActiveOn(today.minus(14)))
        assertTrue(goal.isActiveOn(today.minus(13)))
        assertTrue(goal.isActiveOn(today.plus(7)))
        assertFalse(goal.isActiveOn(today.plus(8)))
    }

    @Test
    fun testScoresAndStreaksStopAtTheEnd() {
        plan(today.minus(28), 20.0, 21.0)
        log(today.minus(28), today, 30)
        val end = today.minus(15)
        assertTrue(goal.scores[end].value > 0.0)
        assertEquals(0.0, goal.scores[end.plus(1)].value)
        val best = goal.streaks.getBest(1)
        assertEquals(end, best[0].end)
    }

    @Test
    fun testIsCompletedToday() {
        plan(today.minus(13), 10.0, 20.0, 30.0)
        log(today.minus(13), today.minus(1), 2)
        assertFalse(goal.isCompletedToday())
        goal.originalEntries.add(Entry(today, 8000))
        goal.recompute()
        assertTrue(goal.isCompletedToday())
    }

    @Test
    fun testIsCompletedToday_endedGoal() {
        plan(today.minus(28), 20.0, 21.0)
        goal.recompute()
        assertTrue(goal.isCompletedToday())
    }

    @Test
    fun testListScore() {
        plan(today.minus(13), 10.0, 20.0, 30.0)
        log(today.minus(13), today, 2)
        assertEquals(28.0 / 60.0, goal.listScore(today), 1e-9)

        val habit = fixtures.createNumericalHabit()
        assertEquals(habit.scores[today].value, habit.listScore(today))
    }
}
