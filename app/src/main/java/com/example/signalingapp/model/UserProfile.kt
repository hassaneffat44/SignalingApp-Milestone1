package com.example.signalingapp.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = ROLE_BUYER, // "Buyer" or "Seller"
    val shopName: String = "",
    val address: String = ""
) {
    companion object {
        const val ROLE_BUYER = "Buyer"
        const val ROLE_SELLER = "Seller"
    }
}
