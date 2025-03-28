package com.example.snapbuy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.snapbuy.adapters.GroceryAdapter
import com.example.snapbuy.models.Grocery
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

class HomeActivity : AppCompatActivity() {

    private lateinit var rvGroceryList: RecyclerView
    private lateinit var btnAddGrocery: FloatingActionButton
    private lateinit var btnScanReceipt: FloatingActionButton
    private lateinit var etSearch: EditText
    private lateinit var tvWelcome: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val groceryList = mutableListOf<Grocery>()
    private lateinit var adapter: GroceryAdapter
    private var groceryListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        rvGroceryList = findViewById(R.id.rvGroceryList)
        btnAddGrocery = findViewById(R.id.btnAddGrocery)
        btnScanReceipt = findViewById(R.id.btnScanReceipt)
        etSearch = findViewById(R.id.etSearch)
        tvWelcome = findViewById(R.id.tvWelcome)

        adapter = GroceryAdapter(this, groceryList)
        rvGroceryList.layoutManager = LinearLayoutManager(this)
        rvGroceryList.adapter = adapter

        // ✅ Check if user is logged in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        loadUserName(currentUser.uid)
        loadGroceries(currentUser.uid)

        // ✅ Request Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    102
                )
            }
        }

        btnAddGrocery.setOnClickListener {
            startActivity(Intent(this, AddGroceryActivity::class.java))
        }

        btnScanReceipt.setOnClickListener {
            startActivity(Intent(this, ScanReceiptActivity::class.java))
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterGroceries(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // ✅ Load groceries from Firestore (Users → userId → Groceries)
    private fun loadGroceries(userId: String) {
        groceryListener?.remove() // Remove old listener to prevent duplicates

        groceryListener = db.collection("Users").document(userId).collection("Groceries")
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    Log.e("Firebase", "Error getting groceries: ", exception)
                    Toast.makeText(this, "Failed to load groceries", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                groceryList.clear()

                snapshots?.forEach { doc ->
                    val grocery = doc.toObject(Grocery::class.java).apply { id = doc.id }
                    groceryList.add(grocery)
                }
                adapter.updateList(groceryList)
            }
    }

    // ✅ Load user's name from Firestore
    private fun loadUserName(userId: String) {
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome) // Ensure a TextView exists in UI
        db.collection("Users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")
                    tvWelcome.text = "Hello, ${name ?: "User"}"
                } else {
                    tvWelcome.text = "Hello, User"
                }
            }
            .addOnFailureListener {
                tvWelcome.text = "Hello, User"
            }
    }


    // ✅ Filter grocery items based on search input
    private fun filterGroceries(query: String) {
        val filteredList = groceryList.filter { it.name.contains(query, ignoreCase = true) }
        adapter.updateList(filteredList)
    }

    override fun onDestroy() {
        super.onDestroy()
        groceryListener?.remove() // Remove Firestore listener to prevent memory leaks
    }

    companion object {
        private const val GOOGLE_SIGN_IN = 1001
    }
}
