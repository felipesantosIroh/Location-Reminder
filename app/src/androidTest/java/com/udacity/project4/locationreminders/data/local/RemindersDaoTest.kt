package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initializeDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insert_getReminderById_ReturnsNotEmpty() = runBlockingTest {
        val reminder1 = ReminderDTO("1", "description 1", "loc 1", 555.6, 555.6)

        database.reminderDao().saveReminder(reminder1)
        val result = database.reminderDao().getReminderById(reminder1.id)

        assertThat(result as ReminderDTO, notNullValue())
        assertThat(result.id, `is`(reminder1.id))
        assertThat(result.title, `is`(reminder1.title))
        assertThat(result.description, `is`(reminder1.description))
        assertThat(result.location, `is`(reminder1.location))
        assertThat(result.latitude, `is`(reminder1.latitude))
        assertThat(result.longitude, `is`(reminder1.longitude))
    }

    @Test
    fun insert_getAllReminders_AllReminders_() = runBlockingTest {
        val reminder1 = ReminderDTO("1", "description 1", "loc 1", 555.6, 555.6)
        val reminder2 = ReminderDTO("2", "description 2", "loc 2", 123.0, 123.0)
        val reminder3 = ReminderDTO("3", "description 3", "loc 3", 456.0, 879.0)
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)

        val reminders = database.reminderDao().getReminders()
        assertThat(reminders, notNullValue())
    }

    @Test
    fun getAllReminders_NoReminders_ReturnsEmptyList() = runBlockingTest {
        val reminders = database.reminderDao().getReminders()
        assertThat(reminders, `is`(emptyList()))
    }

        @Test
    fun insert_delete_AllReminders() = runBlockingTest {
        val reminder1 = ReminderDTO("1", "description 1", "loc 1", 555.6, 555.6)
        val reminder2 = ReminderDTO("2", "description 2", "loc 2", 123.0, 123.0)
        val reminder3 = ReminderDTO("3", "description 3", "loc 3", 456.0, 879.0)
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)

        database.reminderDao().deleteAllReminders()
        val reminders = database.reminderDao().getReminders()
        assertThat(reminders, `is`(emptyList()))
    }
}