package com.kasir.mobile.ui.navigation

object NavRoutes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val POS = "pos"
    const val RENTAL = "rental"
    const val RENTAL_RETURN = "rental_return"
    const val INVENTORY = "inventory"
    const val INVENTORY_ADD = "inventory_add"
    const val INVENTORY_EDIT = "inventory_edit/{itemId}"
    const val SESSION = "session"
    const val DELETION_LOGS = "deletion_logs"
    const val SETTINGS = "settings"

    fun inventoryEdit(itemId: Long) = "inventory_edit/$itemId"
}
