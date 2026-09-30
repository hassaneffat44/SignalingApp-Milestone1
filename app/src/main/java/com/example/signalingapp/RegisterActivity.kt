package com.example.signalingapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.signalingapp.model.UserProfile
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFullName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etShopName: TextInputEditText
    private lateinit var tilShopName: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var rgRole: RadioGroup
    private lateinit var rbBuyer: RadioButton
    private lateinit var rbSeller: RadioButton
    private lateinit var btnRegister: Button
    private lateinit var tvLoginLink: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://signalingapp64-20617-default-rtdb.firebaseio.com")

        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etShopName = findViewById(R.id.etShopName)
        tilShopName = findViewById(R.id.tilShopName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        rgRole = findViewById(R.id.rgRole)
        rbBuyer = findViewById(R.id.rbBuyer)
        rbSeller = findViewById(R.id.rbSeller)
        btnRegister = findViewById(R.id.btnRegister)
        tvLoginLink = findViewById(R.id.tvLoginLink)
        progressBar = findViewById(R.id.progressBar)

        rgRole.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbSeller) {
                tilShopName.visibility = View.VISIBLE
            } else {
                tilShopName.visibility = View.GONE
            }
        }

        btnRegister.setOnClickListener {
            registerUser()
        }

        tvLoginLink.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {
        val fullName = etFullName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val shopName = etShopName.text.toString().trim()
        val role = if (rbSeller.isChecked) UserProfile.ROLE_SELLER else UserProfile.ROLE_BUYER

        if (fullName.isEmpty()) {
            etFullName.error = "Full Name is required"
            etFullName.requestFocus()
            return
        }

        if (phone.isEmpty()) {
            etPhone.error = "Phone Number is required"
            etPhone.requestFocus()
            return
        }

        if (role == UserProfile.ROLE_SELLER && shopName.isEmpty()) {
            etShopName.error = "Shop Name is required for Sellers"
            etShopName.requestFocus()
            return
        }

        if (email.isEmpty()) {
            etEmail.error = "Email Address is required"
            etEmail.requestFocus()
            return
        }

        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            etPassword.requestFocus()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid ?: ""

                    val profile = UserProfile(
                        uid = uid,
                        name = fullName,
                        email = email,
                        phone = phone,
                        role = role,
                        shopName = if (role == UserProfile.ROLE_SELLER) shopName else "",
                        address = ""
                    )

                    var completed = false
                    val handler = Handler(Looper.getMainLooper())
                    val timeoutRunnable = Runnable {
                        if (!completed) {
                            completed = true
                            progressBar.visibility = View.GONE
                            btnRegister.isEnabled = true
                            Toast.makeText(
                                this@RegisterActivity,
                                "Account created! Note: Realtime Database timed out. Make sure Realtime Database is created in Firebase Console.",
                                Toast.LENGTH_LONG
                            ).show()

                            val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    }
                    handler.postDelayed(timeoutRunnable, 6000)

                    // Save User Profile to Firebase Realtime Database
                    database.getReference("users").child(uid).setValue(profile)
                        .addOnSuccessListener {
                            if (!completed) {
                                completed = true
                                handler.removeCallbacks(timeoutRunnable)
                                progressBar.visibility = View.GONE
                                btnRegister.isEnabled = true

                                Toast.makeText(
                                    this,
                                    "Registration successful! Welcome $fullName",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        }
                        .addOnFailureListener { dbError ->
                            if (!completed) {
                                completed = true
                                handler.removeCallbacks(timeoutRunnable)
                                progressBar.visibility = View.GONE
                                btnRegister.isEnabled = true

                                Toast.makeText(
                                    this,
                                    "Database Error: ${dbError.message}",
                                    Toast.LENGTH_LONG
                                ).show()

                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        }
                } else {
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true
                    Toast.makeText(
                        this,
                        "Authentication Failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
