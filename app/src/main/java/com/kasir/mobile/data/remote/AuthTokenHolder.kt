package com.kasir.mobile.data.remote

/**
 * In-memory holder for session tokens.
 *
 * Separates the persistent login token from short-lived admin escalation tokens
 * so an admin PIN verification (10 min TTL) never clobbers the main cashier
 * login session.
 */
object AuthTokenHolder {
    @Volatile
    var loginToken: String? = null

    @Volatile
    var escalationToken: String? = null

    /** Returns the escalation token if active, otherwise the base login token. */
    val activeToken: String?
        get() = escalationToken ?: loginToken

    /** Backward-compatible alias for existing callers. */
    var token: String?
        get() = activeToken
        set(value) {
            loginToken = value
        }

    fun setEscalation(escToken: String?) {
        escalationToken = escToken
    }

    fun clearEscalation() {
        escalationToken = null
    }

    fun clearAll() {
        loginToken = null
        escalationToken = null
    }
}
