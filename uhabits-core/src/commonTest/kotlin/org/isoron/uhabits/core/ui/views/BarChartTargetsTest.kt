package org.isoron.uhabits.core.ui.views

import kotlinx.coroutines.test.runTest
import org.isoron.platform.gui.assertRenders
import org.isoron.platform.io.createTestDateFormatter
import org.isoron.platform.time.LocalDate
import kotlin.test.Test

class BarChartTargetsTest {
    private val today = LocalDate(2015, 1, 25)
    private val theme = LightTheme()
    private val component = BarChart(theme, createTestDateFormatter()).apply {
        axis = (0..100).map { today.minus(it) }
        series.add(listOf(200.0, 0.0, 150.0, 137.0, 0.0, 0.0, 500.0, 30.0, 100.0, 0.0, 300.0))
        colors.add(theme.color(8))
        targets = listOf(600.0, 600.0, 600.0, 400.0, 400.0, 400.0, 400.0, 200.0, 200.0, 200.0, 200.0)
    }

    @Test
    fun testDrawWithTargets() = runTest {
        assertRenders(300, 200, "views/BarChart/targets.png", component)
    }
}
