package org.isoron.uhabits.core.database.migrations

import kotlinx.coroutines.test.runTest
import org.isoron.platform.io.Database
import org.isoron.platform.io.format
import org.isoron.platform.io.migrateTo
import org.isoron.platform.io.query
import org.isoron.uhabits.core.BaseUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Version26Test : BaseUnitTest() {

    private lateinit var db: Database

    private suspend fun initDb() {
        db = openDatabaseResource("/databases/022.db")
    }

    private suspend fun migrateTo(version: Int) {
        db.migrateTo(version) { v ->
            val path = "migrations/${format("%02d.sql", v)}"
            fileOpener.openResourceFile(path).lines().joinToString("\n")
        }
    }

    private fun dbTest(block: suspend () -> Unit) = runTest {
        initDb()
        block()
    }

    @Test
    fun testMigrateTo26CreatesScheduleColumns() = dbTest {
        migrateTo(26)
        var rows = 0
        db.query(
            "select target_schedule_start, target_schedule_stage_length, " +
                "target_schedule_values from Habits"
        ) { stmt ->
            rows++
            assertNull(stmt.getLongOrNull(0))
            assertNull(stmt.getIntOrNull(1))
            assertNull(stmt.getTextOrNull(2))
        }
        assertTrue(rows > 0)
    }

    @Test
    fun testMigrateTo26KeepsExistingTargets() = dbTest {
        val before = mutableListOf<Double?>()
        db.query("select target_value from Habits order by id") { before.add(it.getRealOrNull(0)) }

        migrateTo(26)

        val after = mutableListOf<Double?>()
        db.query("select target_value from Habits order by id") { after.add(it.getRealOrNull(0)) }
        assertEquals(before, after)
    }
}
