package com.kasir.mobile.data.remote

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthTokenHolderTest {

    @Before
    @After
    fun resetTokens() {
        AuthTokenHolder.clearAll()
    }

    @Test
    fun testLoginTokenOnly() {
        AuthTokenHolder.loginToken = "cashier-token-123"
        assertEquals("cashier-token-123", AuthTokenHolder.activeToken)
        assertEquals("cashier-token-123", AuthTokenHolder.token)
    }

    @Test
    fun testEscalationTokenOverridesLoginTokenTemporarily() {
        AuthTokenHolder.loginToken = "cashier-token-123"
        AuthTokenHolder.setEscalation("admin-escalation-456")

        // While escalation is active, activeToken returns the escalation token
        assertEquals("admin-escalation-456", AuthTokenHolder.activeToken)
        assertEquals("admin-escalation-456", AuthTokenHolder.token)

        // Clearing escalation seamlessly restores the login token without clobbering it
        AuthTokenHolder.clearEscalation()
        assertEquals("cashier-token-123", AuthTokenHolder.activeToken)
        assertEquals("cashier-token-123", AuthTokenHolder.token)
    }

    @Test
    fun testClearAllRemovesBothTokens() {
        AuthTokenHolder.loginToken = "login-token"
        AuthTokenHolder.setEscalation("esc-token")
        AuthTokenHolder.clearAll()

        assertNull(AuthTokenHolder.loginToken)
        assertNull(AuthTokenHolder.escalationToken)
        assertNull(AuthTokenHolder.activeToken)
    }
}
