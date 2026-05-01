package com.example.gastosapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.gastosapp.databinding.ActivityRegisterBinding
import com.example.gastosapp.Utils.FirebaseUtils
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
            setLoading(false)
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

    private fun setLoading(loading: Boolean) {
        binding.progressBarRegister.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegistrar.isEnabled = !loading
        binding.registerGoogle.isEnabled = !loading
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        setLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseUtils.auth.signInWithCredential(credential)
            .addOnSuccessListener {
                crearDocumentoUsuarioSiNoExiste()
            }
            .addOnFailureListener {
                setLoading(false)
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

        var valid = true

        if (nombre.isEmpty()) {
            binding.campNombre.error = "El nombre es requerido"
            valid = false
        } else if (nombre.length < 2) {
            binding.campNombre.error = "El nombre debe tener al menos 2 caracteres"
            valid = false
        } else if (nombre.length > 50) {
            binding.campNombre.error = "El nombre no puede superar los 50 caracteres"
            valid = false
        } else if (!nombre.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+$"))) {
            binding.campNombre.error = "El nombre solo puede contener letras y espacios"
            valid = false
        } else {
            binding.campNombre.error = null
        }

        if (email.isEmpty()) {
            binding.campCorreo.error = "El correo es requerido"
            valid = false
        } else {
            binding.campCorreo.error = null
        }

        if (pass1.length < 8) {
            binding.campPassword.error = "La contraseña debe tener al menos 8 caracteres"
            binding.campPassword.isErrorEnabled = true
            valid = false
        } else {
            binding.campPassword.error = null
            binding.campPassword.isErrorEnabled = false
        }

        if (pass1 != pass2) {
            binding.campRPassword.error = "Las contraseñas no coinciden"
            binding.campRPassword.isErrorEnabled = true
            valid = false
        } else {
            binding.campRPassword.error = null
            binding.campRPassword.isErrorEnabled = false
        }

        if (!valid) return

        setLoading(true)
        FirebaseUtils.auth.createUserWithEmailAndPassword(email, pass1)
            .addOnSuccessListener {
                crearDocumentoUsuarioSiNoExiste(nombre)
            }
            .addOnFailureListener {
                setLoading(false)
                Toast.makeText(this, "Error registro: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun iniciarGoogleSignIn() {
        googleSignInClient.signOut()
        googleLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun crearDocumentoUsuarioSiNoExiste(nombreGoogle: String? = null) {
        val uid = FirebaseUtils.uid() ?: run {
            setLoading(false)
            return
        }
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
                    setLoading(false)
                    irAlDashboard()
                }.addOnFailureListener {
                    setLoading(false)
                    Toast.makeText(this, "Error al guardar datos: ${it.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                setLoading(false)
                irAlDashboard()
            }
        }.addOnFailureListener {
            setLoading(false)
            Toast.makeText(this, "Error de conexión: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun irAlDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}