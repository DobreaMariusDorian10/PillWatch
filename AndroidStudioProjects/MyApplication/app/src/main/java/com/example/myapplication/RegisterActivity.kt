package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        initializeDoctorCode()

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etRepeatPassword = findViewById<EditText>(R.id.etRepeatPassword)
        val cbDoctor = findViewById<CheckBox>(R.id.cbDoctor)
        val etDoctorCode = findViewById<EditText>(R.id.etDoctorCode)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnGoToLogin = findViewById<Button>(R.id.btnGoToLogin)

        cbDoctor.setOnCheckedChangeListener { _, isChecked ->
            etDoctorCode.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val repeatPassword = etRepeatPassword.text.toString()
            val isDoctor = cbDoctor.isChecked
            val enteredCode = etDoctorCode.text.toString().trim()

            if (name.isEmpty()) {
                showToast("Completează numele")
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                showToast("Completează email-ul")
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                showToast("Completează parola")
                return@setOnClickListener
            }

            if (repeatPassword.isEmpty()) {
                showToast("Repetă parola")
                return@setOnClickListener
            }

            if (password != repeatPassword) {
                showToast("Parolele nu se potrivesc")
                return@setOnClickListener
            }

            if (isDoctor) {
                if (enteredCode.isEmpty()) {
                    showToast("Introdu codul de doctor")
                    return@setOnClickListener
                }

                validateDoctorCode(enteredCode) { isValid ->
                    if (isValid) {
                        registerUser(name, email, password, "doctor")
                    } else {
                        showToast("Codul de doctor este invalid")
                    }
                }

            } else {
                registerUser(name, email, password, "")
            }
        }

        btnGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun initializeDoctorCode() {
        val ref = FirebaseDatabase.getInstance().getReference("access_codes/codDoctor")
        ref.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                ref.setValue("DOCTOR")
            }
        }.addOnFailureListener {
            showToast("Eroare la inițializarea codului de doctor.")
        }
    }

    private fun validateDoctorCode(codeEntered: String, callback: (Boolean) -> Unit) {
        val codeRef = FirebaseDatabase.getInstance().getReference("access_codes/codDoctor")
        codeRef.get().addOnSuccessListener { snapshot ->
            val validCode = snapshot.getValue(String::class.java)
            val isValid = !validCode.isNullOrEmpty() && codeEntered == validCode
            callback(isValid)
        }.addOnFailureListener {
            showToast("Eroare la citirea codului de doctor.")
            callback(false)
        }
    }

    private fun registerUser(name: String, email: String, password: String, role: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            val userRef = FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(user.uid)

                            val userData = mapOf(
                                "uid" to user.uid,
                                "name" to name,
                                "email" to email,
                                "role" to role
                            )

                            userRef.setValue(userData).addOnSuccessListener {
                                showToast("Înregistrare reușită!")
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }.addOnFailureListener {
                                showToast("Eroare la salvarea datelor în baza de date.")
                            }
                        } else {
                            showToast("Nu am putut actualiza profilul utilizatorului.")
                        }
                    }
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        showEmailExistsDialog()
                    } else {
                        showToast("Eroare la înregistrare: ${exception?.message}")
                    }
                }
            }
    }

    private fun showEmailExistsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Adresă deja folosită")
            .setMessage("Există deja un cont asociat acestei adrese de email. Te rugăm să te autentifici sau să folosești un alt email.")
            .setPositiveButton("Autentificare") { _, _ ->
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Închide", null)
            .setCancelable(true)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
