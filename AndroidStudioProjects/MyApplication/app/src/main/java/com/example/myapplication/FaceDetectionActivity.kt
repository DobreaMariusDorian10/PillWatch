package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.io.IOException

class FaceDetectionActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var detectButton: Button
    private lateinit var resultText: TextView
    private var selectedBitmap: Bitmap? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val imageUri: Uri? = data?.data
                imageUri?.let {
                    selectedBitmap = getBitmapFromUri(it)
                    imageView.setImageBitmap(selectedBitmap)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detection)

        imageView = findViewById(R.id.imageView)
        detectButton = findViewById(R.id.detectButton)
        resultText = findViewById(R.id.resultText)

        imageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        detectButton.setOnClickListener {
            selectedBitmap?.let {
                detectFaceWithFacePlusPlus(it)
            } ?: Toast.makeText(this, "Selectează o imagine mai întâi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun detectFaceWithFacePlusPlus(bitmap: Bitmap) {
        val apiKey = "7Sf8DbvXBH3CDC4fdAQ7zhKb0uwBM_VB"
        val apiSecret = "adefQdMN6ryELzb-NIOP556NydcGCJ1w"
        val base64Image = bitmapToBase64(bitmap)

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("api_key", apiKey)
            .addFormDataPart("api_secret", apiSecret)
            .addFormDataPart("image_base64", base64Image)
            .addFormDataPart("return_attributes", "gender,age,smiling")
            .build()

        val request = Request.Builder()
            .url("https://api-us.faceplusplus.com/facepp/v3/detect")
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    resultText.text = "Eroare: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string()
                Handler(Looper.getMainLooper()).post {
                    if (json != null && json.contains("faces") && json.contains("face_token")) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        resultText.text = "Nicio față detectată."
                    }
                }
            }
        })
    }
}
