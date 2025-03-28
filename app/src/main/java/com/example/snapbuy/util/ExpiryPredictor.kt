package com.example.snapbuy.util

object ExpiryPredictor {
    fun predictExpiry(groceryName: String): String {
        return when (groceryName.lowercase()) {
            "milk" -> "2025-03-12"  // 5 days
            "bread" -> "2025-03-10"  // 3 days
            "apple" -> "2025-03-20"  // 10 days
            "carrot" -> "2025-03-25"  // 15 days
            else -> "2025-04-01"  // Default expiry
        }
    }
}
