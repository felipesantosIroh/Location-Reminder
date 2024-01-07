package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var reminderFakeRepository: FakeDataSource
    private lateinit var viewModel: RemindersListViewModel

    private lateinit var reminder1: ReminderDTO
    private lateinit var reminder2: ReminderDTO
    private lateinit var reminder3: ReminderDTO

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun init() {
        reminder1 = ReminderDTO("1", "description 1", "loc 1", 555.6, 555.6)
        reminder2 = ReminderDTO("2", "description 2", "loc 2", 123.0, 123.0)
        reminder3 = ReminderDTO("3", "description 3", "loc 3", 456.0, 879.0)
        reminderFakeRepository = FakeDataSource()
        viewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(), reminderFakeRepository
        )
    }

    @After
    fun cleanUp(){
        stopKoin()
    }

    @Test
    fun loadRemindersCheck() = mainCoroutineRule.runBlockingTest{
        reminderFakeRepository.saveReminder(reminder1)
        reminderFakeRepository.saveReminder(reminder2)

        mainCoroutineRule.pauseDispatcher()

        viewModel.loadReminders()

        mainCoroutineRule.resumeDispatcher()

        assertThat(viewModel.remindersList.getOrAwaitValue().size, `is`(2))
    }

    @Test
    fun emptyListReminders_shouldReturnError() = mainCoroutineRule.runBlockingTest {

        reminderFakeRepository.deleteAllReminders()

        reminderFakeRepository.setReturnError(true)

        viewModel.loadReminders()

        assertThat(viewModel.showSnackBar.getOrAwaitValue(), `is`("Could not get reminders"))
    }

    @Test
    fun loadReminders_loadingCheck() = mainCoroutineRule.runBlockingTest{
        mainCoroutineRule.pauseDispatcher()
        reminderFakeRepository.deleteAllReminders()

        viewModel.loadReminders()

        assertThat(
            viewModel.showLoading.value, `is`(true)
        )

        mainCoroutineRule.resumeDispatcher()
        assertThat(
            viewModel.showLoading.value, `is`(false)
        )

    }
}