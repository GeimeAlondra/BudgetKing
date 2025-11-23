package com.example.gastosapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.airbnb.lottie.LottieAnimationView
import com.example.gastosapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var animationCash: LottieAnimationView
    private var isAnimating = false
    private var isExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

//        enableEdgeToEdge()
        setContentView(binding.root)

        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser != null){
            irInicioActivity()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.CardViewMain.setOnClickListener {
            isExpanded = !isExpanded
            abrirCard()
        }

        setupAnimationCash()
        binding.btnLogin.setOnClickListener {
            if (firebaseAuth.currentUser != null){
                irInicioActivity()
            }
            else{
                startActivity(Intent(applicationContext, LoginActivity::class.java))
//                finish()
            }
        }

        binding.btnMainRegistrar.setOnClickListener {
            startActivity(Intent(applicationContext, RegisterActivity::class.java))
//            finish()
        }
    }

    private fun abrirCard() {
        val descripcion = binding.tvDescripcionCashControl
        val cardContent = binding.cardContent

        descripcion.visibility = if (isExpanded) View.VISIBLE else View.GONE

        TransitionManager.beginDelayedTransition(cardContent, AutoTransition().apply {
            duration = 300
            addTarget(descripcion)
        })
    }

    private fun setupAnimationCash() {
        animationCash = findViewById(R.id.animationCash)

        animationCash.setOnClickListener {
//            animationCash.progress = 0f
            if (!isAnimating) {
                animationCash.playAnimation()
                isAnimating = true

                animationCash.isClickable = false
                Handler(Looper.getMainLooper()).postDelayed({
                    animationCash.isClickable = true
                    isAnimating = false
                }, 1000)
            }
        }
    }

    private fun irInicioActivity() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

}

