package com.kasir.mobile.ui.viewmodel

import com.kasir.mobile.data.ServiceLocator
import com.kasir.mobile.data.model.ActionSuccessResponse
import com.kasir.mobile.data.model.FetchAllDataResponse
import com.kasir.mobile.data.model.ItemDto
import com.kasir.mobile.data.remote.AuthTokenHolder
import com.kasir.mobile.data.repository.KasirRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class KasirViewModelStartRentalTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repo = mockk<KasirRepository>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        AuthTokenHolder.clearAll()
        coEvery { repo.fetchAllData() } returns Result.success(FetchAllDataResponse())
        ServiceLocator.setRepositoryForTesting(repo)
    }

    @After
    fun tearDown() {
        ServiceLocator.setRepositoryForTesting(null)
        Dispatchers.resetMain()
    }

    private fun startAndCollect(vm: KasirViewModel, results: MutableList<Boolean>) =
        vm.startRental("Budi", listOf(ItemDto(code = "X", qty = 1)), "cash") { results += it }

    @Test
    fun reportsFailureOnNetworkError() = runTest(testDispatcher) {
        coEvery { repo.addSession(any()) } returns Result.failure(IOException("offline"))
        val vm = KasirViewModel(testDispatcher).apply { togglePrintMulai(false) }
        val results = mutableListOf<Boolean>()
        startAndCollect(vm, results)
        advanceUntilIdle()
        assertEquals(listOf(false), results)
    }

    @Test
    fun reportsFailureWhenServerRejects() = runTest(testDispatcher) {
        coEvery { repo.addSession(any()) } returns Result.success(ActionSuccessResponse(success = false, error = "x"))
        val vm = KasirViewModel(testDispatcher).apply { togglePrintMulai(false) }
        val results = mutableListOf<Boolean>()
        startAndCollect(vm, results)
        advanceUntilIdle()
        assertEquals(listOf(false), results)
    }

    @Test
    fun reportsSuccessWhenServerAccepts() = runTest(testDispatcher) {
        coEvery { repo.addSession(any()) } returns Result.success(ActionSuccessResponse(success = true))
        val vm = KasirViewModel(testDispatcher).apply { togglePrintMulai(false) }
        val results = mutableListOf<Boolean>()
        startAndCollect(vm, results)
        advanceUntilIdle()
        assertEquals(listOf(true), results)
    }
}
