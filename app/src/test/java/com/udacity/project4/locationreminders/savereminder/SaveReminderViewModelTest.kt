package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private lateinit var repository: FakeDataSource
    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var context: Application

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun init(){
        stopKoin()
        context = getApplicationContext()
        repository = FakeDataSource()
        viewModel = SaveReminderViewModel(context, repository)
    }

    @Test
    fun reminderClear_reminderNullValues() {
        viewModel.reminderTitle.value = "1"
        viewModel.reminderDescription.value = "description 1"
        viewModel.reminderSelectedLocationStr.value = "loc 1"
        viewModel.selectedPOI.value = PointOfInterest(LatLng(555.6, 555.6), "Manaus", "Amazon")
        viewModel.latitude.value = 1.0
        viewModel.longitude.value = 2.0

        viewModel.onClear()

        assertNull(viewModel.reminderTitle.value)
        assertNull(viewModel.reminderDescription.value)
        assertNull(viewModel.reminderSelectedLocationStr.value)
        assertNull(viewModel.selectedPOI.value)
        assertNull(viewModel.latitude.value)
        assertNull(viewModel.longitude.value)
    }

    @Test
    fun createReminder_validateSaveReminder() {
        val fakeReminder = ReminderDataItem("1", "description 1", "loc 1", 555.6, 555.6)

        viewModel.saveReminder(fakeReminder)

        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun validateEnteredData_validData_returnTrue() {
        val fakeReminder = ReminderDataItem("1", "description 1", "loc 1", 555.6, 555.6)

        val validateDate = viewModel.validateEnteredData(fakeReminder)

        assertThat(validateDate, `is`(true))
    }


}