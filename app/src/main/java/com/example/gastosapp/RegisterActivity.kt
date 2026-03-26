package com.example.gastosapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.gastosapp.databinding.ActivityRegisterBinding
import com.example.gastosapp.utils.FirebaseUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: Exception) {
            Toast.makeText(this, "Error Google: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        setupGoogleSignIn()

        binding.btnRegistrar.setOnClickListener { registrarConEmail() }
        binding.registerGoogle.setOnClickListener { iniciarGoogleSignIn() }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseUtils.auth.signInWithCredential(credential)
            .addOnSuccessListener {
                crearDocumentoUsuarioSiNoExiste()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error autenticación: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun registrarConEmail() {
        val nombre = binding.rNombre.text.toString().trim()
        val email = binding.rCorreo.text.toString().trim()
        val pass1 = binding.rPassword.text.toString()
        val pass2 = binding.rRPassword.text.toString()

        if (nombre.isEmpty() || email.isEmpty() || pass1.isEmpty() || pass2.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        if (pass1 != pass2) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseUtils.auth.createUserWithEmailAndPassword(email, pass1)
            .addOnSuccessListener {
                crearDocumentoUsuarioSiNoExiste(nombre)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error registro: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun iniciarGoogleSignIn() {
        googleSignInClient.signOut()
        googleLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun crearDocumentoUsuarioSiNoExiste(nombreGoogle: String? = null) {
        val uid = FirebaseUtils.uid() ?: return
        val docRef = FirebaseUtils.db.collection("usuarios").document(uid)

        docRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val nombre = nombreGoogle ?: binding.rNombre.text.toString().trim()
                val data = hashMapOf(
                    "nombre" to nombre,
                    "proveedor" to if (nombreGoogle != null) "Google" else "Email",
                    "creadoEn" to System.currentTimeMillis()
                )
                docRef.set(data).addOnSuccessListener {
                    irAlDashboard()
                }
            } else {
                irAlDashboard()
            }
        }
    }

    private fun irAlDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}