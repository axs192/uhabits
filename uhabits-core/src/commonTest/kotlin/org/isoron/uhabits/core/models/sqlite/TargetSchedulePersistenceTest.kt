package org.isoron.uhabits.core.models.sqlite

import kotlinx.coroutines.test.runTest
import org.isoron.platform.io.TestDatabaseHelper
import org.isoron.platform.io.migrateTo
import org.isoron.platform.io.setVersion
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.DATABASE_VERSION
import org.isoron.uhabits.core.database.HabitData
import org.isoron.uhabits.core.database.HabitRepository
import org.isoron.uhabits.core.io.LoopDBImporter
import org.isoron.uhabits.core.io.StandardLogging
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.TargetSchedule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TargetSchedulePersistenceTest : BaseUnitTest() {
    private val schedule = TargetSchedule(
        start = LocalDate(2015, 1, 5),
        stageLength = 7,
        values = listOf(10.0, 20.0, 25.5)
    )

    @Test
    fun testScheduleSurvivesSaveAndReload() = runTest {
        val factory = SQLModelFactory(buildMemoryDatabase())
        val list = SQLiteHabitList(factory)
        val habit = factory.buildHabit()
        habit.name = "Pushups"
        habit.type = HabitType.NUMERICAL
        habit.targetValue = 10.0
        habit.targetSchedule = schedule
        list.add(habit)

        val reloaded = SQLiteHabitList(factory).getById(habit.id!!)!!
        assertEquals(schedule, reloaded.targetSchedule)
        assertEquals(10.0, reloaded.targetValue)
    }

    @Test
    fun testScheduleCanBeUpdatedAndCleared() = runTest {
        val factory = SQLModelFactory(buildMemoryDatabase())
        val list = SQLiteHabitList(factory)
        val habit = factory.buildHabit()
        habit.type = HabitType.NUMERICAL
        list.add(habit)
        assertNull(SQLiteHabitList(factory).getById(habit.id!!)!!.targetSchedule)

        habit.targetSchedule = schedule
        list.update(habit)
        assertEquals(schedule, SQLiteHabitList(factory).getById(habit.id!!)!!.targetSchedule)

        habit.targetSchedule = null
        list.update(habit)
        assertNull(SQLiteHabitList(factory).getById(habit.id!!)!!.targetSchedule)
    }

    @Test
    fun testRepositoryStoresScheduleColumns() = runTest {
        val db = buildMemoryDatabase()
        val repo = HabitRepository(db)
        val id = repo.insert(
            HabitData(
                name = "Pushups",
                position = 0,
                targetScheduleStart = schedule.start.unixTime,
                targetScheduleStageLength = 7,
                targetScheduleValues = "10;20"
            )
        )
        repo.insert(HabitData(name = "Plain", position = 1))

        val all = repo.findAll()
        assertEquals(schedule.start.unixTime, all[0].targetScheduleStart)
        assertEquals(7, all[0].targetScheduleStageLength)
        assertEquals("10;20", all[0].targetScheduleValues)
        assertNull(all[1].targetScheduleStart)
        assertNull(all[1].targetScheduleStageLength)
        assertNull(all[1].targetScheduleValues)

        val updated = all[0].copy(name = "Renamed", targetScheduleValues = "5")
        repo.update(updated)
        val afterUpdate = repo.findAll().first { it.id == id }
        assertEquals("Renamed", afterUpdate.name)
        assertEquals("5", afterUpdate.targetScheduleValues)
        db.close()
    }

    @Test
    fun testScheduleSurvivesBackupImport() = runTest {
        val file = fileOpener.openUserFile("test-temp-schedule-backup.db")
        if (file.exists()) file.delete()
        val backup = databaseOpener().open(file.pathString)
        backup.setVersion(8)
        backup.migrateTo(DATABASE_VERSION) { v -> TestDatabaseHelper.loadMigrationSQL(v) }
        HabitRepository(backup).insert(
            HabitData(
                name = "Pushups",
                position = 0,
                type = HabitType.NUMERICAL.value,
                targetValue = 10.0,
                uuid = "schedule-import-test",
                targetScheduleStart = schedule.start.unixTime,
                targetScheduleStageLength = schedule.stageLength,
                targetScheduleValues = schedule.serializeValues()
            )
        )
        backup.close()

        val importer = LoopDBImporter(
            habitList,
            modelFactory,
            databaseOpener(),
            commandRunner,
            StandardLogging(),
            fileOpener
        )
        assertTrue(importer.canHandle(file))
        importer.importHabitsFromFile(file)
        file.delete()

        val imported = habitList.getByUUID("schedule-import-test")!!
        assertEquals(schedule, imported.targetSchedule)
    }
}
