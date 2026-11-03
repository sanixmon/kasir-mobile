package com.kasir.mobile.ui.viewmodel

import com.kasir.mobile.data.ServiceLocator
import com.kasir.mobile.data.model.ActionSuccessResponse
import com.kasir.mobile.data.model.FetchAllDataResponse
import com.kasir.mobile.data.model.SessionDto
import com.kasir.mobile.data.model.TransactionDto
import com.kasir.mobile.data.model.VerifyAdminResponse
import com.kasir.mobile.data.remote.AuthTokenHolder
import com.kasir.mobile.data.repository.KasirRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * The admin escalation token must stay attached for the whole protected
 * request — not be cleared as soon as the request coroutine is launched.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KasirViewModelAdminEscalationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repo = mockk<KasirRepository>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        AuthTokenHolder.clearAll()
        AuthTokenHolder.loginToken = "cashier-token"
        coEvery { repo.fetchAllData() } returns Result.success(FetchAllDataResponse())
        coEvery { repo.verifyAdmin(any()) } returns Result.success(VerifyAdminResponse(valid = true, token = "admin-esc"))
        coEvery { repo.addDeletionLog(any()) } returns Result.success(ActionSuccessResponse(success = true))
        ServiceLocator.setRepositoryForTesting(repo)
    }

    @After
    fun tearDown() {
        ServiceLocator.setRepositoryForTesting(null)
        Dispatchers.resetMain()
        AuthTokenHolder.clearAll()
    }

    @Test
    fun deleteTransactionRunsWithEscalationTokenThenClearsIt() = runTest(testDispatcher) {
        var tokenDuringDelete: String? = null
        coEvery { repo.deleteTxn(any(), any()) } coAnswers {
            delay(50) // simulate network latency
            tokenDuringDelete = AuthTokenHolder.activeToken
            Result.success(ActionSuccessResponse(success = true))
        }

        val vm = KasirViewModel()
        vm.setShiftUser("kasir1", "cashier")
        vm.deleteTransaction(TransactionDto(id = "t-1", no = 1))
        vm.verifyAdminPin("1234")
        advanceUntilIdle()

        assertEquals("admin-esc", tokenDuringDelete)
        assertNull(AuthTokenHolder.escalationToken)
        assertEquals("cashier-token", AuthTokenHolder.activeToken)
    }

    @Test
    fun editSessionSaveUsesTokenGrantedWhenDialogOpened() = runTest(testDispatcher) {
        var tokenDuringEdit: String? = null
        coEvery { repo.editSession(any()) } coAnswers {
            tokenDuringEdit = AuthTokenHolder.activeToken
            Result.success(ActionSuccessResponse(success = true))
        }
        val session = SessionDto(id = "s-1", nama = "Budi")

        val vm = KasirViewModel()
        vm.setShiftUser("kasir1", "cashier")
        vm.requestEditSession(session)
        vm.verifyAdminPin("1234")
        advanceUntilIdle()

        // Dialog open; escalation is not left attached to unrelated traffic.
        assertEquals(session, vm.activeEditSession.value)
        assertEquals("cashier-token", AuthTokenHolder.activeToken)

        vm.saveEditedSession(session.copy(nama = "Budi S"))
        advanceUntilIdle()

        assertEquals("admin-esc", tokenDuringEdit)
        assertNull(vm.activeEditSession.value)
        assertEquals("cashier-token", AuthTokenHolder.activeToken)
    }

    @Test
    fun expiredEditEscalationDoesNotLogOutCashier() = runTest(testDispatcher) {
        val http401 = retrofit2.HttpException(
            retrofit2.Response.error<Any>(401, okhttp3.ResponseBody.Companion.create(null, ""))
        )
        coEvery { repo.editSession(any()) } returns Result.failure(http401)
        val session = SessionDto(id = "s-1", nama = "Budi")

        val vm = KasirViewModel()
        vm.setShiftUser("kasir1", "cashier")
        vm.requestEditSession(session)
        vm.verifyAdminPin("1234")
        advanceUntilIdle()
        vm.saveEditedSession(session)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.sessionExpired)
        assertEquals("kasir1", vm.uiState.value.currentShiftUser)
        assertNull(vm.activeEditSession.value)
    }
}

class TransactionIdentityTest {
    @Test
    fun matchesByIdAndNeverLumpsTogetherZeroNumberedTxns() {
        val a = TransactionDto(id = "t-a", no = 0)
        val b = TransactionDto(id = "t-b", no = 0)
        org.junit.Assert.assertTrue(a.isSameTxn(a.copy()))
        assertFalse(a.isSameTxn(b))
        assertFalse(TransactionDto(id = "", no = 0).isSameTxn(TransactionDto(id = "", no = 0)))
        org.junit.Assert.assertTrue(TransactionDto(id = "", no = 7).isSameTxn(TransactionDto(id = "x", no = 7)))
    }
}
