package com.kasir.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ItemDto(
    val code: String,
    val qty: Int = 1
)

@Serializable
data class CatalogItem(
    val code: String,
    val name: String,
    val emoji: String,
    val defaultImg: String,
    val priceHour: Double,
    val priceOT30: Double,
    val priceOT60: Double,
    val isPackage: Boolean = false,
    val packageHours: Int = 1
)

object ItemCatalog {
    val ITEMS = listOf(
        CatalogItem("ST", "Stroller", "🛺", "https://i.ibb.co.com/fzwMy2XL/The-Edit-The-stroller-changing-the-game-banner-desktop.webp", 20000.0, 10000.0, 20000.0),
        CatalogItem("SB", "Stroller Paket 3J", "🛺", "https://i.ibb.co.com/fzwMy2XL/The-Edit-The-stroller-changing-the-game-banner-desktop.webp", 50000.0, 10000.0, 20000.0, isPackage = true, packageHours = 3),
        CatalogItem("SD", "Scooter Dewasa", "🛵", "https://i.ibb.co.com/rG55b6ts/wp8922917.jpg", 50000.0, 25000.0, 50000.0),
        CatalogItem("SJ", "Scooter Jumbo", "🦽", "https://i.ibb.co.com/hxVgMw63/Pngtree-3d-render-of-a-black-5598024.jpg", 60000.0, 30000.0, 60000.0),
        CatalogItem("SA", "Scooter Anak", "🛴", "https://i.ibb.co.com/qMZ9szQQ/adad.png", 35000.0, 20000.0, 35000.0)
    )

    fun findByCode(code: String): CatalogItem? = ITEMS.find { it.code.equals(code, ignoreCase = true) }
}
