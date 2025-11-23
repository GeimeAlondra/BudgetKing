package com.example.gastosapp.utils

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

object FirebaseUtils {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val db: FirebaseFirestore = Firebase.firestore

    fun uid(): String? = auth.currentUser?.uid
    fun email(): String? = auth.currentUser?.email
    fun displayName(): String? = auth.currentUser?.displayName

    fun isLoggedIn(): Boolean = auth.currentUser != null
}