package com.kasir.mobile.ui.viewmodel

import com.kasir.mobile.data.remote.AuthTokenHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KasirViewModelSessionTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        AuthTokenHolder.clearAll()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        AuthTokenHolder.clearAll()
    }

    @Test
    fun testLogoutClearsShiftUserRoleAndAuthToken() {
        val vm = KasirViewModel()
        vm.setShiftUser("kasir1", "cashier")
        AuthTokenHolder.loginToken = "test-token-123"

        assertEquals("kasir1", vm.uiState.value.currentShiftUser)
        assertEquals("cashier", vm.uiState.value.currentUserRole)
        assertEquals("test-token-123", AuthTokenHolder.activeToken)

        vm.logout()

        assertNull(vm.uiState.value.currentShiftUser)
        assertNull(vm.uiState.value.currentUserRole)
        assertNull(AuthTokenHolder.activeToken)
        assertNull(AuthTokenHolder.loginToken)
    }

    @Test
    fun testHandleSessionExpiredSetsFlagAndClearsUser() {
        val vm = KasirViewModel()
        vm.setShiftUser("kasir1", "cashier")
        AuthTokenHolder.loginToken = "expired-token-456"

        assertFalse(vm.uiState.value.sessionExpired)

        vm.handleSessionExpired()

        assertTrue(vm.uiState.value.sessionExpired)
        assertNull(vm.uiState.value.currentShiftUser)
        assertNull(vm.uiState.value.currentUserRole)
        assertFalse(vm.uiState.value.apiConnected)
        assertEquals("Sesi login berakhir. Silakan login kembali.", vm.uiState.value.errorMessage)
        assertNull(AuthTokenHolder.activeToken)
    }

    @Test
    fun testResetSessionExpiredClearsFlag() {
        val vm = KasirViewModel()
        vm.handleSessionExpired()
        assertTrue(vm.uiState.value.sessionExpired)

        vm.resetSessionExpired()
        assertFalse(vm.uiState.value.sessionExpired)
    }
}
