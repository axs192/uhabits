/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.activities.habits.edit

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Bundle
import android.text.Html
import android.text.InputType
import android.text.Spanned
import android.text.format.DateFormat
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.android.datetimepicker.time.RadialPickerLayout
import com.android.datetimepicker.time.TimePickerDialog
import org.isoron.platform.gui.toInt
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.activities.common.dialogs.ColorPickerDialogFactory
import org.isoron.uhabits.activities.common.dialogs.FrequencyPickerDialog
import org.isoron.uhabits.activities.common.dialogs.WeekdayPickerDialog
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.CreateHabitCommand
import org.isoron.uhabits.core.commands.EditHabitCommand
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.TargetSchedule
import org.isoron.uhabits.core.models.WeekdayList
import org.isoron.uhabits.databinding.ActivityEditHabitBinding
import org.isoron.uhabits.databinding.EditHabitStageRowBinding
import org.isoron.uhabits.utils.applyBottomInset
import org.isoron.uhabits.utils.applyRootViewInsets
import org.isoron.uhabits.utils.applyToolbarInsets
import org.isoron.uhabits.utils.dismissCurrentAndShow
import org.isoron.uhabits.utils.formatTime
import org.isoron.uhabits.utils.toFormattedString
import java.util.Date
import java.util.TimeZone

fun formatFrequency(freqNum: Int, freqDen: Int, resources: Resources) = when {
    freqNum == 1 && (freqDen == 30 || freqDen == 31) -> resources.getString(R.string.every_month)
    freqDen == 30 || freqDen == 31 -> resources.getString(R.string.x_times_per_month, freqNum)
    freqNum == 1 && freqDen == 1 -> resources.getString(R.string.every_day)
    freqNum == 1 && freqDen == 7 -> resources.getString(R.string.every_week)
    freqNum == 1 && freqDen > 1 -> resources.getString(R.string.every_x_days, freqDen)
    freqDen == 7 -> resources.getString(R.string.x_times_per_week, freqNum)
    else -> resources.getString(R.string.x_times_per_y_days, freqNum, freqDen)
}

class EditHabitActivity : AppCompatActivity() {

    private lateinit var themeSwitcher: AndroidThemeSwitcher
    private lateinit var binding: ActivityEditHabitBinding
    private lateinit var commandRunner: CommandRunner

    var habitId = -1L
    lateinit var habitType: HabitType
    var unit = ""
    var color = PaletteColor(11)
    var androidColor = 0
    var freqNum = 1
    var freqDen = 1
    var reminderHour = -1
    var reminderMin = -1
    var reminderDays: WeekdayList = WeekdayList.EVERY_DAY
    var targetType = NumericalHabitType.AT_LEAST
    var isProgressive = false
    var scheduleStart: LocalDate = getToday()
    var stageLength = 7
    private val stageRows = mutableListOf<EditHabitStageRowBinding>()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        var stageTexts = listOf<String>()

        val component = (application as HabitsApplication).component
        themeSwitcher = AndroidThemeSwitcher(this, component.preferences)
        themeSwitcher.apply()

        binding = ActivityEditHabitBinding.inflate(layoutInflater)
        binding.root.applyRootViewInsets()
        binding.root.applyBottomInset()
        binding.toolbar.applyToolbarInsets()
        setContentView(binding.root)

        if (intent.hasExtra("habitId")) {
            binding.toolbar.title = getString(R.string.edit_habit)
            habitId = intent.getLongExtra("habitId", -1)
            val habit = component.habitList.getById(habitId)!!
            habitType = habit.type
            color = habit.color
            freqNum = habit.frequency.numerator
            freqDen = habit.frequency.denominator
            targetType = habit.targetType
            habit.reminder?.let {
                reminderHour = it.hour
                reminderMin = it.minute
                reminderDays = it.days
            }
            binding.nameInput.setText(habit.name)
            binding.questionInput.setText(habit.question)
            binding.notesInput.setText(habit.description)
            binding.unitInput.setText(habit.unit)
            binding.targetInput.setText(habit.targetValue.toString())
            habit.targetSchedule?.let {
                isProgressive = true
                scheduleStart = it.start
                stageLength = it.stageLength
                stageTexts = it.values.map { value -> formatStageValue(value) }
            }
        } else {
            habitType = HabitType.fromInt(intent.getIntExtra("habitType", HabitType.YES_NO.value))
        }

        if (state != null) {
            habitId = state.getLong("habitId")
            habitType = HabitType.fromInt(state.getInt("habitType"))
            color = PaletteColor(state.getInt("paletteColor"))
            freqNum = state.getInt("freqNum")
            freqDen = state.getInt("freqDen")
            reminderHour = state.getInt("reminderHour")
            reminderMin = state.getInt("reminderMin")
            reminderDays = WeekdayList(state.getInt("reminderDays"))
            isProgressive = state.getBoolean("isProgressive")
            scheduleStart = LocalDate(state.getInt("scheduleStart"))
            stageLength = state.getInt("stageLength")
            stageTexts = state.getStringArrayList("stageTexts") ?: listOf()
        }

        updateColors()

        when (habitType) {
            HabitType.YES_NO -> {
                binding.unitOuterBox.visibility = View.GONE
                binding.targetOuterBox.visibility = View.GONE
                binding.targetTypeOuterBox.visibility = View.GONE
                binding.progressiveOuterBox.visibility = View.GONE
                binding.scheduleOuterBox.visibility = View.GONE
            }
            HabitType.NUMERICAL -> {
                binding.nameInput.hint = getString(R.string.measurable_short_example)
                binding.questionInput.hint = getString(R.string.measurable_question_example)
                binding.frequencyOuterBox.visibility = View.GONE
            }
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.elevation = 10.0f

        val colorPickerDialogFactory = ColorPickerDialogFactory(this)
        binding.colorButton.setOnClickListener {
            val picker = colorPickerDialogFactory.create(color, themeSwitcher.currentTheme)
            picker.setListener { paletteColor ->
                this.color = paletteColor
                updateColors()
            }
            picker.dismissCurrentAndShow(supportFragmentManager, "colorPicker")
        }

        populateFrequency()
        binding.booleanFrequencyPicker.setOnClickListener {
            val picker = FrequencyPickerDialog(freqNum, freqDen)
            picker.onFrequencyPicked = { num, den ->
                freqNum = num
                freqDen = den
                populateFrequency()
            }
            picker.dismissCurrentAndShow(supportFragmentManager, "frequencyPicker")
        }

        populateTargetType()
        binding.targetTypePicker.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_item)
            arrayAdapter.add(getString(R.string.target_type_at_least))
            arrayAdapter.add(getString(R.string.target_type_at_most))
            builder.setAdapter(arrayAdapter) { dialog, which ->
                targetType = when (which) {
                    0 -> NumericalHabitType.AT_LEAST
                    else -> NumericalHabitType.AT_MOST
                }
                populateTargetType()
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.dismissCurrentAndShow()
        }

        stageTexts.forEach { addStageRow(it) }
        populateSchedule()
        binding.progressivePicker.setOnClickListener {
            isProgressive = !isProgressive
            if (isProgressive && stageRows.isEmpty()) {
                addStageRow(binding.targetInput.text.toString())
            }
            populateSchedule()
        }
        binding.scheduleStartPicker.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    scheduleStart = LocalDate(year, month + 1, day)
                    populateSchedule()
                },
                scheduleStart.year,
                scheduleStart.month - 1,
                scheduleStart.day
            ).show()
        }
        binding.stageLengthPicker.setOnClickListener { showStageLengthPicker() }
        binding.addStageButton.setOnClickListener {
            addStageRow("").stageInput.requestFocus()
            populateSchedule()
        }

        binding.numericalFrequencyPicker.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_item)
            arrayAdapter.add(getString(R.string.every_day))
            arrayAdapter.add(getString(R.string.every_week))
            arrayAdapter.add(getString(R.string.every_month))
            builder.setAdapter(arrayAdapter) { dialog, which ->
                freqDen = when (which) {
                    1 -> 7
                    2 -> 30
                    else -> 1
                }
                populateFrequency()
                dialog.dismiss()
            }
            builder.show()
        }

        populateReminder()
        binding.reminderTimePicker.setOnClickListener {
            val currentHour = if (reminderHour >= 0) reminderHour else 8
            val currentMin = if (reminderMin >= 0) reminderMin else 0
            val is24HourMode = DateFormat.is24HourFormat(this)
            val dialog = TimePickerDialog.newInstance(
                object : TimePickerDialog.OnTimeSetListener {
                    override fun onTimeSet(view: RadialPickerLayout?, hourOfDay: Int, minute: Int) {
                        reminderHour = hourOfDay
                        reminderMin = minute
                        populateReminder()
                    }

                    override fun onTimeCleared(view: RadialPickerLayout?) {
                        reminderHour = -1
                        reminderMin = -1
                        reminderDays = WeekdayList.EVERY_DAY
                        populateReminder()
                    }
                },
                currentHour,
                currentMin,
                is24HourMode,
                androidColor
            )
            dialog.dismissCurrentAndShow(supportFragmentManager, "timePicker")
        }

        binding.reminderDatePicker.setOnClickListener {
            val dialog = WeekdayPickerDialog()

            dialog.setListener { days: WeekdayList ->
                reminderDays = days
                if (reminderDays.isEmpty) reminderDays = WeekdayList.EVERY_DAY
                populateReminder()
            }
            dialog.setSelectedDays(reminderDays)
            dialog.dismissCurrentAndShow(supportFragmentManager, "dayPicker")
        }

        binding.buttonSave.setOnClickListener {
            if (validate()) save()
        }

        for (fragment in supportFragmentManager.fragments) {
            (fragment as DialogFragment).dismiss()
        }
    }

    private fun save() {
        val component = (application as HabitsApplication).component
        val habit = component.modelFactory.buildHabit()

        var original: Habit? = null
        if (habitId >= 0) {
            original = component.habitList.getById(habitId)!!
            habit.copyFrom(original)
        }

        habit.name = binding.nameInput.text.trim().toString()
        habit.question = binding.questionInput.text.trim().toString()
        habit.description = binding.notesInput.text.trim().toString()
        habit.color = color
        if (reminderHour >= 0) {
            habit.reminder = Reminder(reminderHour, reminderMin, reminderDays)
        } else {
            habit.reminder = null
        }

        habit.frequency = Frequency(freqNum, freqDen)
        if (habitType == HabitType.NUMERICAL) {
            if (isProgressive) {
                val values = stageRows.map { it.stageInput.text.toString().toDouble() }
                habit.targetSchedule = TargetSchedule(scheduleStart, stageLength, values)
                habit.targetValue = values.first()
            } else {
                habit.targetSchedule = null
                habit.targetValue = binding.targetInput.text.toString().toDouble()
            }
            habit.targetType = targetType
            habit.unit = binding.unitInput.text.trim().toString()
        }
        habit.type = habitType

        val command = if (habitId >= 0) {
            EditHabitCommand(
                component.habitList,
                habitId,
                habit
            )
        } else {
            CreateHabitCommand(
                component.modelFactory,
                component.habitList,
                habit
            )
        }
        component.commandRunner.run(command)
        finish()
    }

    private fun validate(): Boolean {
        var isValid = true
        if (binding.nameInput.text.isEmpty()) {
            binding.nameInput.error = getFormattedValidationError(R.string.validation_cannot_be_blank)
            isValid = false
        }
        if (habitType == HabitType.NUMERICAL && isProgressive) {
            for (row in stageRows) {
                if (row.stageInput.text.toString().toDoubleOrNull() == null) {
                    row.stageInput.error = getString(R.string.validation_cannot_be_blank)
                    isValid = false
                }
            }
        } else if (habitType == HabitType.NUMERICAL) {
            if (binding.targetInput.text.isEmpty()) {
                binding.targetInput.error = getString(R.string.validation_cannot_be_blank)
                isValid = false
            }
        }
        return isValid
    }

    private fun populateReminder() {
        if (reminderHour < 0) {
            binding.reminderTimePicker.text = getString(R.string.reminder_off)
            binding.reminderDatePicker.visibility = View.GONE
            binding.reminderDivider.visibility = View.GONE
        } else {
            val time = formatTime(this, reminderHour, reminderMin)
            binding.reminderTimePicker.text = time
            binding.reminderDatePicker.visibility = View.VISIBLE
            binding.reminderDivider.visibility = View.VISIBLE
            binding.reminderDatePicker.text = reminderDays.toFormattedString(this)
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun populateFrequency() {
        binding.booleanFrequencyPicker.text = formatFrequency(freqNum, freqDen, resources)
        binding.numericalFrequencyPicker.text = when (freqDen) {
            1 -> getString(R.string.every_day)
            7 -> getString(R.string.every_week)
            30 -> getString(R.string.every_month)
            else -> "$freqNum/$freqDen"
        }
    }

    private fun populateSchedule() {
        binding.progressivePicker.text = getString(
            if (isProgressive) R.string.progressive_target_on else R.string.progressive_target_off
        )
        if (habitType != HabitType.NUMERICAL) return
        if (isProgressive && stageRows.isEmpty()) addStageRow("")
        binding.scheduleOuterBox.visibility = if (isProgressive) View.VISIBLE else View.GONE
        binding.targetValueBox.visibility = if (isProgressive) View.GONE else View.VISIBLE

        val dateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        binding.scheduleStartPicker.text = dateFormat.format(Date(scheduleStart.unixTime))
        binding.stageLengthPicker.text = formatStageLength(stageLength)

        val labelRes = if (stageLength == 7) R.string.schedule_week_n else R.string.schedule_stage_n
        stageRows.forEachIndexed { index, row ->
            row.stageLabel.text = getString(labelRes, index + 1)
            row.stageRemoveButton.visibility = if (stageRows.size > 1) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun addStageRow(text: String): EditHabitStageRowBinding {
        val row = EditHabitStageRowBinding.inflate(layoutInflater, binding.stagesContainer, true)
        row.stageInput.setText(text)
        row.stageRemoveButton.setOnClickListener {
            stageRows.remove(row)
            binding.stagesContainer.removeView(row.root)
            populateSchedule()
        }
        stageRows.add(row)
        return row
    }

    private fun formatStageValue(value: Double): String {
        return if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    }

    private fun formatStageLength(days: Int) = when (days) {
        1 -> getString(R.string.schedule_length_one_day)
        7 -> getString(R.string.schedule_length_one_week)
        14 -> getString(R.string.schedule_length_two_weeks)
        28 -> getString(R.string.schedule_length_four_weeks)
        else -> getString(R.string.schedule_length_days, days)
    }

    private fun showStageLengthPicker() {
        val presets = intArrayOf(1, 7, 14, 28)
        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_item)
        presets.forEach { arrayAdapter.add(formatStageLength(it)) }
        arrayAdapter.add(getString(R.string.schedule_length_custom))
        AlertDialog.Builder(this)
            .setAdapter(arrayAdapter) { dialog, which ->
                if (which < presets.size) {
                    stageLength = presets[which]
                    populateSchedule()
                } else {
                    showCustomStageLengthDialog()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showCustomStageLengthDialog() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setText(stageLength.toString())
        AlertDialog.Builder(this)
            .setTitle(R.string.schedule_length_custom_title)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val days = input.text.toString().toIntOrNull()
                if (days != null && days > 0) {
                    stageLength = days
                    populateSchedule()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun populateTargetType() {
        binding.targetTypePicker.text = when (targetType) {
            NumericalHabitType.AT_MOST -> getString(R.string.target_type_at_most)
            else -> getString(R.string.target_type_at_least)
        }
    }

    private fun updateColors() {
        androidColor = themeSwitcher.currentTheme.color(color).toInt()
        binding.colorButton.backgroundTintList = ColorStateList.valueOf(androidColor)
        if (!themeSwitcher.isNightMode) {
            window.statusBarColor = androidColor
            binding.toolbar.setBackgroundColor(androidColor)
        }
    }

    private fun getFormattedValidationError(@StringRes resId: Int): Spanned {
        val html = "<font color=#FFFFFF>${getString(resId)}</font>"
        return Html.fromHtml(html)
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        with(state) {
            putLong("habitId", habitId)
            putInt("habitType", habitType.value)
            putInt("paletteColor", color.paletteIndex)
            putInt("androidColor", androidColor)
            putInt("freqNum", freqNum)
            putInt("freqDen", freqDen)
            putInt("reminderHour", reminderHour)
            putInt("reminderMin", reminderMin)
            putInt("reminderDays", reminderDays.toInteger())
            putBoolean("isProgressive", isProgressive)
            putInt("scheduleStart", scheduleStart.daysSince2000)
            putInt("stageLength", stageLength)
            putStringArrayList("stageTexts", ArrayList(stageRows.map { it.stageInput.text.toString() }))
        }
    }
}
