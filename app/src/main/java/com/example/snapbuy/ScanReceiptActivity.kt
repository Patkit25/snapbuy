package com.example.snapbuy

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.snapbuy.models.Grocery
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanReceiptActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private val db = FirebaseFirestore.getInstance()
    private lateinit var previewView: androidx.camera.view.PreviewView

    companion object {
        private const val CAMERA_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_receipt)

        // ✅ Prevent unauthorized access
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        previewView = findViewById(R.id.previewView)
        val btnScan = findViewById<FloatingActionButton>(R.id.btnScan)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // ✅ Request Camera Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            startCamera()
        }

        btnScan.setOnClickListener {
            captureAndProcessImage()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraX", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndProcessImage() {
        val imageCapture = imageCapture ?: run {
            Toast.makeText(this, "Camera not initialized!", Toast.LENGTH_SHORT).show()
            return
        }

        val photoFile = File(cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d("CameraX", "Photo captured: $savedUri")
                    processImage(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Image capture failed", exception)
                    Toast.makeText(this@ScanReceiptActivity, "Failed to capture image", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun processImage(imageUri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = visionText.text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    val groceryItems = mutableListOf<Grocery>()

                    for (line in lines) {
                        val words = line.split(" ").filter { it.isNotEmpty() }
                        if (words.isNotEmpty()) {
                            val name = words.joinToString(" ")
                            val quantity = "1"
                            val expiryDate = calculateExpiryDate(name)

                            val grocery = Grocery("", name, quantity, expiryDate, "General")
                            groceryItems.add(grocery)
                        }
                    }

                    saveToFirestore(groceryItems)
                }
                .addOnFailureListener { e ->
                    Log.e("TextRecognition", "Error processing text", e)
                    Toast.makeText(this, "Failed to scan receipt", Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception) {
            Log.e("TextRecognition", "Error reading image", e)
        }
    }

    private fun calculateExpiryDate(itemName: String): String {
        val calendar = Calendar.getInstance()
        val expiryDays = when {
            itemName.contains("milk", ignoreCase = true) -> 7
            itemName.contains("bread", ignoreCase = true) -> 5
            itemName.contains("fruit", ignoreCase = true) -> 4
            itemName.contains("vegetable", ignoreCase = true) -> 6
            itemName.contains("cheese", ignoreCase = true) -> 15
            itemName.contains("yogurt", ignoreCase = true) -> 10
            itemName.contains("eggs", ignoreCase = true) -> 21
            itemName.contains("meat", ignoreCase = true) -> 7
            else -> 30
        }
        calendar.add(Calendar.DAY_OF_YEAR, expiryDays)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    private fun saveToFirestore(groceryItems: List<Grocery>) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userUID = user.uid
        val collection = db.collection("Users").document(userUID).collection("Groceries")

        for (grocery in groceryItems) {
            collection.add(grocery)
                .addOnSuccessListener {
                    Log.d("Firestore", "Grocery added: ${grocery.name}")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error adding grocery", e)
                }
        }

        Toast.makeText(this, "Items saved successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
