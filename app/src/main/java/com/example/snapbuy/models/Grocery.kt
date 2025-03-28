package com.example.snapbuy.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Grocery(
    @get:Exclude var id: String = "",  // Exclude from Firestore to avoid duplication
    var name: String = "",
    var quantity: String = "",
    var expiryDate: String? = null,  // Expiry date can be null
    var category: String = "General",  // Default category
    var barcode: String? = null,  // Optional barcode
    @ServerTimestamp var timestamp: Date? = null
)
