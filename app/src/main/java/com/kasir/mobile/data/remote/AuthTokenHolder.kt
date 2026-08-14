package com.kasir.mobile.data.remote

/**
 * In-memory holder for the server-issued session token.
 *
 * The token is set at login (cashier or admin) and injected into every API
 * request by [RetrofitClient]'s auth interceptor. It lives only in memory, so
 * it is reset when the app process restarts — the app always starts at the
 * login screen, so a fresh login re-issues the token.
 */
object AuthTokenHolder {
    @Volatile
    var token: String? = null
}
