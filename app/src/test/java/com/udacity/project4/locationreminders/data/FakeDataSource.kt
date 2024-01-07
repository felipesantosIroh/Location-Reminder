package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(val remindersMutableList: MutableList<ReminderDTO> = mutableListOf()) :
    ReminderDataSource {

    private var returnError: Boolean = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (returnError) {
             Result.Error("Could not get reminders")
        } else {
            Result.Success(remindersMutableList)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersMutableList.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return if (returnError) {
            Result.Error("Could not get reminder, some error occurs")
        } else {
            val reminder = remindersMutableList.find { it.id == id }
            reminder?.let { Result.Success(reminder) } ?: run {
                Result.Error("Could not get reminder by id")
            }
        }
    }

    override suspend fun deleteAllReminders() {
        remindersMutableList.clear()
    }

    fun setReturnError(shouldReturnError: Boolean) {
        this.returnError = shouldReturnError
    }
}