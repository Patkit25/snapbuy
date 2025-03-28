package com.example.snapbuy

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.snapbuy.models.Grocery
import com.example.snapbuy.util.ExpiryPredictor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddGroceryActivity : AppCompatActivity() {
    private lateinit var etGroceryName: EditText
    private lateinit var etQuantity: EditText
    private lateinit var etCategory: EditText
    private lateinit var etBarcode: EditText
    private lateinit var btnSave: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_grocery)

        etGroceryName = findViewById(R.id.etGroceryName)
        etQuantity = findViewById(R.id.etQuantity)
        etCategory = findViewById(R.id.etCategory)
        etBarcode = findViewById(R.id.etBarcode)
        btnSave = findViewById(R.id.btnSave)

        btnSave.setOnClickListener {
            saveGroceryToFirestore()
        }
    }

    private fun saveGroceryToFirestore() {
        val name = etGroceryName.text.toString().trim()
        val quantity = etQuantity.text.toString().trim()
        val category = etCategory.text.toString().trim()
        val barcode = etBarcode.text.toString().trim()

        // ✅ Validate Input
        if (name.isEmpty() || quantity.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Enter all details!", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Get current user ID
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Predict expiry date using AI model
        val expiryDate = ExpiryPredictor.predictExpiry(name)

        // ✅ Create Firestore Document ID manually
        val groceryId = db.collection("Users").document(userId)
            .collection("Groceries").document().id

        // ✅ Create Grocery Object
        val grocery = Grocery(groceryId, name, quantity, expiryDate, category, barcode)

        // ✅ Save grocery under the user's Groceries subcollection
        db.collection("Users").document(userId)
            .collection("Groceries").document(groceryId)
            .set(grocery)
            .addOnSuccessListener {
                Toast.makeText(this, "Grocery Added!", Toast.LENGTH_SHORT).show()
                finish() // Close the activity after successful addition
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
