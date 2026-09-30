package org.isoron.uhabits.core.models

import org.isoron.platform.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TargetScheduleTest {
    private val start = LocalDate(2024, 1, 1)
    private val schedule = TargetSchedule(
        start = start,
        stageLength = 7,
        values = listOf(10.0, 20.0, 25.0)
    )

    @Test
    fun testStageIndexOn_beforeStart() {
        assertNull(schedule.stageIndexOn(start.minus(1)))
    }

    @Test
    fun testStageIndexOn_duringSchedule() {
        assertEquals(0, schedule.stageIndexOn(start))
        assertEquals(0, schedule.stageIndexOn(start.plus(6)))
        assertEquals(1, schedule.stageIndexOn(start.plus(7)))
        assertEquals(1, schedule.stageIndexOn(start.plus(13)))
        assertEquals(2, schedule.stageIndexOn(start.plus(14)))
    }

    @Test
    fun testStageIndexOn_afterLastStage_holdsLastStage() {
        assertEquals(2, schedule.stageIndexOn(start.plus(21)))
        assertEquals(2, schedule.stageIndexOn(start.plus(1000)))
    }

    @Test
    fun testValueOn() {
        assertEquals(3.0, schedule.valueOn(start.minus(1), fallback = 3.0))
        assertEquals(10.0, schedule.valueOn(start, fallback = 3.0))
        assertEquals(20.0, schedule.valueOn(start.plus(7), fallback = 3.0))
        assertEquals(25.0, schedule.valueOn(start.plus(14), fallback = 3.0))
        assertEquals(25.0, schedule.valueOn(start.plus(500), fallback = 3.0))
    }

    @Test
    fun testStageStart() {
        assertEquals(start, schedule.stageStart(0))
        assertEquals(start.plus(7), schedule.stageStart(1))
        assertEquals(start.plus(14), schedule.stageStart(2))
    }

    @Test
    fun testNextChange() {
        assertEquals(Pair(start, 10.0), schedule.nextChange(start.minus(3)))
        assertEquals(Pair(start.plus(7), 20.0), schedule.nextChange(start))
        assertEquals(Pair(start.plus(7), 20.0), schedule.nextChange(start.plus(6)))
        assertEquals(Pair(start.plus(14), 25.0), schedule.nextChange(start.plus(7)))
        assertNull(schedule.nextChange(start.plus(14)))
        assertNull(schedule.nextChange(start.plus(500)))
    }

    @Test
    fun testSerializeValues_roundTrip() {
        val fractional = TargetSchedule(start, 7, listOf(1.5, 2.0, 1000.25))
        assertEquals(fractional.values, TargetSchedule.parseValues(fractional.serializeValues()))
    }

    @Test
    fun testParseValues() {
        assertEquals(listOf(10.0, 20.0, 25.5), TargetSchedule.parseValues("10;20.0;25.5"))
        assertEquals(listOf(10.0), TargetSchedule.parseValues("10"))
        assertEquals(listOf(), TargetSchedule.parseValues(""))
        assertEquals(listOf(10.0, 20.0), TargetSchedule.parseValues("10;abc;20"))
    }

    @Test
    fun testRejectsInvalidSchedules() {
        assertFailsWith<IllegalArgumentException> { TargetSchedule(start, 0, listOf(1.0)) }
        assertFailsWith<IllegalArgumentException> { TargetSchedule(start, 7, listOf()) }
    }
}
