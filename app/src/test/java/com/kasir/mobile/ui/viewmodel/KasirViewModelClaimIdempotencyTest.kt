package com.kasir.mobile.ui.viewmodel

import com.kasir.mobile.data.ServiceLocator
import com.kasir.mobile.data.model.FetchAllDataResponse
import com.kasir.mobile.data.model.SessionDto
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
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.net.SocketTimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
class KasirViewModelClaimIdempotencyTest {

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

    @Test
    fun retryingAFailedClaimReusesTheSameClaimId() = runTest(testDispatcher) {
        val sentClaimIds = mutableListOf<Any?>()
        coEvery { repo.claimSession(any()) } coAnswers {
            sentClaimIds += firstArg<Map<String, Any?>>()["claimId"]
            Result.failure(SocketTimeoutException("timeout"))
        }
        val vm = KasirViewModel(testDispatcher)
        val payment = vm.preparePayment(SessionDto(id = "s-1", nama = "Budi"))

        vm.finalizePayment(payment, cash = 0.0, qris = 0.0)
        advanceUntilIdle()
        vm.finalizePayment(payment, cash = 0.0, qris = 0.0)
        advanceUntilIdle()

        assertEquals(2, sentClaimIds.size)
        assertEquals(sentClaimIds[0], sentClaimIds[1])
    }

    @Test
    fun claimIdSurvivesReturnQtyChangesButDiffersPerPayment() {
        val vm = KasirViewModel(testDispatcher)
        val session = SessionDto(id = "s-1", nama = "Budi")
        val a = vm.preparePayment(session)
        val b = vm.preparePayment(session)

        assertEquals(a.claimId, vm.updateReturnQty(a, 0, 0).claimId)
        assertNotEquals(a.claimId, b.claimId)
    }
}
