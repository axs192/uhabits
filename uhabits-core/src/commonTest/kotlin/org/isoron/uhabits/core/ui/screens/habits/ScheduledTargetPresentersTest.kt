package org.isoron.uhabits.core.ui.screens.habits

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.TargetSchedule
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.callbacks.NumberPickerCallback
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsBehavior
import org.isoron.uhabits.core.ui.screens.habits.show.views.HistoryCardPresenter
import org.isoron.uhabits.core.ui.screens.habits.show.views.SubtitleCardPresenter
import org.isoron.uhabits.core.ui.views.HistoryChart.Square.GREY
import org.isoron.uhabits.core.ui.views.HistoryChart.Square.ON
import org.isoron.uhabits.core.ui.views.LightTheme
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Daily numerical habit with 10 logged on each of the last fourteen days.
 * The schedule asks for 10 during the first week and 20 during the second.
 */
class ScheduledTargetPresentersTest : BaseUnitTest() {
    private lateinit var habit: Habit
    private lateinit var today: LocalDate
    private val screen: ListHabitsBehavior.Screen = mock(MockMode.autoUnit)

    @BeforeTest
    override fun setUp() {
        super.setUp()
        today = getToday()
        habit = fixtures.createEmptyNumericalHabit(NumericalHabitType.AT_LEAST)
        habit.frequency = Frequency.DAILY
        habit.targetValue = 10.0
        habit.targetSchedule = TargetSchedule(
            start = today.minus(13),
            stageLength = 7,
            values = listOf(10.0, 20.0)
        )
        for (i in 0..13) habit.originalEntries.add(Entry(today.minus(i), 10_000))
        habit.recompute()
        habitList.add(habit)
    }

    @Test
    fun testSubtitleShowsTodaysTarget() {
        val state = SubtitleCardPresenter.buildState(habit, LightTheme())
        assertEquals(20.0, state.targetValue)
    }

    @Test
    fun testHistoryColoursEachDayAgainstItsOwnTarget() {
        val state = HistoryCardPresenter.buildState(habit, DayOfWeek.SUNDAY, LightTheme())
        assertEquals(List(7) { GREY } + List(7) { ON }, state.series)
    }

    @Test
    fun testEditingAGoalAsksForANumber() {
        habit.type = HabitType.GOAL
        val behavior = ListHabitsBehavior(
            habitList,
            mock<ListHabitsBehavior.DirFinder>(),
            taskRunner,
            screen,
            commandRunner,
            mock<Preferences>(),
            mock<ListHabitsBehavior.BugReporter>()
        )
        behavior.onEdit(habit, today, 0f, 0f)
        verify { screen.showNumberPopup(10.0, "", any()) }
        verify(VerifyMode.not) { screen.showCheckmarkPopup(any(), any(), any(), any()) }
    }

    @Test
    fun testConfettiUsesTargetOfEditedDate() {
        var picker: NumberPickerCallback? = null
        every { screen.showNumberPopup(any(), any(), any()) } calls { args ->
            picker = args.arg<NumberPickerCallback>(2)
            Unit
        }
        val behavior = ListHabitsBehavior(
            habitList,
            mock<ListHabitsBehavior.DirFinder>(),
            taskRunner,
            screen,
            commandRunner,
            mock<Preferences>(),
            mock<ListHabitsBehavior.BugReporter>()
        )

        // 15 meets last week's target of 10, but not this week's target of 20
        behavior.onEdit(habit, today, 0f, 0f)
        picker!!.onNumberPicked(15.0, "")
        verify(VerifyMode.not) { screen.showConfetti(any(), any(), any()) }

        behavior.onEdit(habit, today.minus(10), 0f, 0f)
        picker!!.onNumberPicked(15.0, "")
        verify(VerifyMode.exactly(1)) { screen.showConfetti(any(), any(), any()) }
    }
}
