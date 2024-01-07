package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private lateinit var remindersDatabase: RemindersDatabase

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
        remindersLocalRepository =
            RemindersLocalRepository(
                remindersDatabase.reminderDao(),
                Dispatchers.Main
            )
    }

    @After
    fun closeDb() {
        remindersDatabase.close()
    }

    @Test
    fun insertReminder_getReminderById_ReturnsSuccess() = runBlocking {
        val reminder1 = ReminderDTO("1", "description 1", "loc 1", 555.6, 555.6)
        remindersLocalRepository.saveReminder(reminder1)
        val result = remindersLocalRepository.getReminder(reminder1.id) as? Result.Success
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success

        assertThat(result.data.title, `is`(reminder1.title))
        assertThat(result.data.description, `is`(reminder1.description))
        assertThat(result.data.latitude, `is`(reminder1.latitude))
        assertThat(result.data.longitude, `is`(reminder1.longitude))
        assertThat(result.data.location, `is`(reminder1.location))
    }

    @Test
    fun getAllReminders_ReturnsSuccessNotEmpty() = runBlocking {
        val reminder1 = ReminderDTO("1", "description 1", "loc 1", 555.6, 555.6)
        val reminder2 = ReminderDTO("2", "description 2", "loc 2", 123.0, 123.0)
        val reminder3 = ReminderDTO("3", "description 3", "loc 3", 456.0, 879.0)
        remindersDatabase.reminderDao().saveReminder(reminder1)
        remindersDatabase.reminderDao().saveReminder(reminder2)
        remindersDatabase.reminderDao().saveReminder(reminder3)
        val result = remindersLocalRepository.getReminders()
        assertThat(result is Result.Success, `is`(true))
        if (result is Result.Success) assertThat(result.data.isNotEmpty(), `is`(true))
    }

    @Test
    fun insertReminder_deleteAllReminders_getReminderById_ReturnsError() = runBlocking {
        val reminder1 = ReminderDTO("1", "description 1", "loc 1", 555.6, 555.6)
        remindersLocalRepository.saveReminder(reminder1)
        remindersLocalRepository.deleteAllReminders()
        val result = remindersLocalRepository.getReminder(reminder1.id)
        assertThat(result is Result.Error, `is`(true))
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }
}