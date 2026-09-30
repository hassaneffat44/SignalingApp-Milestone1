package com.example.signalingapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.signalingapp.model.UserProfile
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvRoleBadge: TextView
    private lateinit var tvEmail: TextView
    private lateinit var etFullName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etShopName: TextInputEditText
    private lateinit var tilShopName: TextInputLayout
    private lateinit var etAddress: TextInputEditText
    private lateinit var btnSaveProfile: Button
    private lateinit var btnSignOut: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var currentUserProfile: UserProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://signalingapp64-20617-default-rtdb.firebaseio.com")

        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_main)

        tvWelcome = findViewById(R.id.tvWelcome)
        tvRoleBadge = findViewById(R.id.tvRoleBadge)
        tvEmail = findViewById(R.id.tvEmail)
        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etShopName = findViewById(R.id.etShopName)
        tilShopName = findViewById(R.id.tilShopName)
        etAddress = findViewById(R.id.etAddress)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        btnSignOut = findViewById(R.id.btnSignOut)
        progressBar = findViewById(R.id.progressBar)

        tvEmail.text = firebaseUser.email ?: ""

        loadUserProfile(firebaseUser.uid)

        btnSaveProfile.setOnClickListener {
            saveUserProfile()
        }

        btnSignOut.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }
    }

    private fun loadUserProfile(uid: String) {
        progressBar.visibility = View.VISIBLE

        var loaded = false
        val handler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!loaded) {
                loaded = true
                progressBar.visibility = View.GONE
                tvWelcome.text = "Welcome!"
                tvEmail.text = auth.currentUser?.email ?: ""
            }
        }
        handler.postDelayed(timeoutRunnable, 5000)

        val userRef = database.getReference("users").child(uid)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!loaded) {
                    loaded = true
                    handler.removeCallbacks(timeoutRunnable)
                    progressBar.visibility = View.GONE
                    val profile = snapshot.getValue(UserProfile::class.java)
                    if (profile != null) {
                        currentUserProfile = profile
                        bindProfileToUi(profile)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!loaded) {
                    loaded = true
                    handler.removeCallbacks(timeoutRunnable)
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to load profile: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun bindProfileToUi(profile: UserProfile) {
        tvWelcome.text = "Welcome, ${profile.name}!"
        tvRoleBadge.text = profile.role.uppercase()

        if (profile.role == UserProfile.ROLE_SELLER) {
            tvRoleBadge.setBackgroundResource(android.R.color.holo_green_dark)
            tilShopName.visibility = View.VISIBLE
            etShopName.setText(profile.shopName)
        } else {
            tvRoleBadge.setBackgroundResource(android.R.color.holo_blue_dark)
            tilShopName.visibility = View.GONE
        }

        etFullName.setText(profile.name)
        etPhone.setText(profile.phone)
        etAddress.setText(profile.address)
    }

    private fun saveUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        val newName = etFullName.text.toString().trim()
        val newPhone = etPhone.text.toString().trim()
        val newAddress = etAddress.text.toString().trim()
        val newShopName = etShopName.text.toString().trim()

        if (newName.isEmpty()) {
            etFullName.error = "Name cannot be empty"
            return
        }

        if (newPhone.isEmpty()) {
            etPhone.error = "Phone number cannot be empty"
            return
        }

        val updatedProfile = currentUserProfile?.copy(
            name = newName,
            phone = newPhone,
            address = newAddress,
            shopName = if (currentUserProfile?.role == UserProfile.ROLE_SELLER) newShopName else ""
        ) ?: UserProfile(
            uid = uid,
            name = newName,
            email = auth.currentUser?.email ?: "",
            phone = newPhone,
            role = UserProfile.ROLE_BUYER,
            shopName = newShopName,
            address = newAddress
        )

        progressBar.visibility = View.VISIBLE
        btnSaveProfile.isEnabled = false

        var saved = false
        val handler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!saved) {
                saved = true
                progressBar.visibility = View.GONE
                btnSaveProfile.isEnabled = true
                Toast.makeText(
                    this@MainActivity,
                    "Database save timed out. Please check Realtime Database connection & rules in Firebase Console.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        handler.postDelayed(timeoutRunnable, 5000)

        database.getReference("users").child(uid).setValue(updatedProfile)
            .addOnSuccessListener {
                if (!saved) {
                    saved = true
                    handler.removeCallbacks(timeoutRunnable)
                    progressBar.visibility = View.GONE
                    btnSaveProfile.isEnabled = true
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                if (!saved) {
                    saved = true
                    handler.removeCallbacks(timeoutRunnable)
                    progressBar.visibility = View.GONE
                    btnSaveProfile.isEnabled = true
                    Toast.makeText(
                        this,
                        "Update failed: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
