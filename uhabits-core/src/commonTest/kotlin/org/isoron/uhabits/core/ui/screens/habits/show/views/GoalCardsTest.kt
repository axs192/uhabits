package org.isoron.uhabits.core.ui.screens.habits.show.views

import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.GoalStatus
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.TargetSchedule
import org.isoron.uhabits.core.ui.views.LightTheme
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Today is Sunday 2015-01-25. The goal runs in weekly stages from Monday
 * Jan 12 (10, then 20, then 30), with 2 logged every day since it began.
 */
class GoalCardsTest : BaseUnitTest() {
    private lateinit var goal: Habit
    private lateinit var today: LocalDate
    private val theme = LightTheme()
    private val monday = 2

    @BeforeTest
    override fun setUp() {
        super.setUp()
        today = getToday()
        goal = fixtures.createEmptyNumericalHabit(NumericalHabitType.AT_LEAST)
        goal.type = HabitType.GOAL
        goal.frequency = Frequency(1, 7)
        goal.targetSchedule = TargetSchedule(today.minus(13), 7, listOf(10.0, 20.0, 30.0))
        for (i in 0..13) goal.originalEntries.add(Entry(today.minus(i), 2000))
        goal.recompute()
    }

    @Test
    fun testGoalCard() {
        val state = GoalCardPresenter.buildState(goal, theme)
        assertTrue(state.isVisible)
        assertEquals(GoalStatus.IN_PROGRESS, state.status)
        assertEquals(2, state.stageNumber)
        assertEquals(3, state.stageCount)
        assertEquals(7, state.stageLength)
        assertEquals(today.plus(7), state.endDate)
        assertEquals(8, state.daysLeft)
        assertEquals(listOf(10.0, 20.0, 30.0), state.stageTargets)
        assertEquals(listOf(14.0, 14.0, 0.0), state.stageActuals)
        assertEquals(60.0, state.totalTarget)
        assertEquals(28.0, state.totalActual)
        assertEquals(1, state.stagesMet)
        assertEquals("miles", state.unit)
    }

    @Test
    fun testGoalCard_hiddenForHabits() {
        val habit = fixtures.createNumericalHabit()
        assertFalse(GoalCardPresenter.buildState(habit, theme).isVisible)
    }

    @Test
    fun testBarCard_weeklyBucketsShowStageTargets() {
        val state = BarCardPresenter.buildState(goal, monday, 1, 0, theme)
        assertEquals(listOf(today.minus(6), today.minus(13)), state.entries.map { it.date })
        assertEquals(listOf(20.0, 10.0), state.targets)
    }

    @Test
    fun testBarCard_dailyBucketsShowDailyShare() {
        val state = BarCardPresenter.buildState(goal, monday, 0, 0, theme)
        val expected = List(7) { 20.0 / 7 } + List(7) { 10.0 / 7 }
        assertEquals(expected.size, state.targets.size)
        expected.zip(state.targets).forEach { (e, a) -> assertEquals(e, a, 1e-9) }
    }

    @Test
    fun testBarCard_habitsHaveNoTargetLine() {
        val habit = fixtures.createNumericalHabit()
        assertTrue(BarCardPresenter.buildState(habit, monday, 1, 0, theme).targets.isEmpty())
    }
}
