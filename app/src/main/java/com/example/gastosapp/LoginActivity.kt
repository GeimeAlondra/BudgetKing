package com.example.gastosapp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.gastosapp.databinding.ActivityLoginBinding
import com.example.gastosapp.Utils.FirebaseUtils
import com.example.gastosapp.db.AppDatabase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleClient: GoogleSignInClient

    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d("LOGIN_DEBUG", "Cuenta Google obtenida: ${account.email}")
            authWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.e("LOGIN_DEBUG", "ApiException código: ${e.statusCode}", e)
            setLoading(false)
            when (e.statusCode) {
                10 -> Toast.makeText(this, "DEVELOPER ERROR - SHA-1 NO REGISTRADO EN FIREBASE", Toast.LENGTH_LONG).show()
                12500 -> Toast.makeText(this, "Google Sign-In cancelado o error interno", Toast.LENGTH_LONG).show()
                7 -> Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_LONG).show()
                else -> Toast.makeText(this, "Error Google ApiException: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("LOGIN_DEBUG", "Error desconocido en launcher", e)
            setLoading(false)
            Toast.makeText(this, "Error fatal: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        if (FirebaseUtils.isLoggedIn()) {
            irDashboard()
            return
        }

        setupGoogle()

        binding.btnLogin.setOnClickListener {
            val email = binding.textCorreo.text.toString().trim()
            val pass = binding.textPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Complete los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.CorreoElectronico.error = "Formato de correo inválido"
                return@setOnClickListener
            } else {
                binding.CorreoElectronico.error = null
            }

            setLoading(true)
            FirebaseUtils.auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    setLoading(false)
                    irDashboard()
                }
                .addOnFailureListener {
                    setLoading(false)
                    Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnLoginGoogle.setOnClickListener {
            setLoading(true)
            googleLauncher.launch(googleClient.signInIntent)
        }

        binding.sinCuenta.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
        binding.tvOlvidaste.setOnClickListener { mostrarDialogoRecuperarContrasena() }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBarLogin.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnLoginGoogle.isEnabled = !loading
    }

    private fun mostrarDialogoRecuperarContrasena() {
        val input = EditText(this).apply {
            hint = "Ingresa tu correo"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(48, 24, 48, 24)
        }

        AlertDialog.Builder(this)
            .setTitle("Recuperar contraseña")
            .setMessage("Te enviaremos un correo para restablecer tu contraseña.")
            .setView(input)
            .setPositiveButton("Enviar") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isEmpty()) {
                    Toast.makeText(this, "Ingresa tu correo", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                FirebaseUtils.auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Correo enviado. Revisa tu bandeja de entrada.", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "No se encontró una cuenta con ese correo.", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)
    }

    private fun authWithGoogle(idToken: String) {
        Log.d("LOGIN_DEBUG", "ID Token recibido: $idToken")

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseUtils.auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = FirebaseUtils.auth.currentUser?.uid ?: run {
                        setLoading(false)
                        return@addOnCompleteListener
                    }
                    Log.d("LOGIN_DEBUG", "¡LOGIN CON GOOGLE EXITOSO! UID: $uid")

                    FirebaseUtils.db.collection("usuarios").document(uid).get()
                        .addOnSuccessListener { doc ->
                            setLoading(false)
                            if (doc.exists()) {
                                irDashboard()
                            } else {
                                FirebaseUtils.auth.signOut()
                                Toast.makeText(this, "No tienes una cuenta. Por favor regístrate primero.", Toast.LENGTH_LONG).show()
                                startActivity(Intent(this, RegisterActivity::class.java))
                                finish()
                            }
                        }
                        .addOnFailureListener {
                            setLoading(false)
                            Log.e("LOGIN_DEBUG", "Error consultando Firestore", it)
                            Toast.makeText(this, "Error al verificar la cuenta: ${it.message}", Toast.LENGTH_LONG).show()
                            FirebaseUtils.auth.signOut()
                        }
                } else {
                    setLoading(false)
                    val error = task.exception
                    Log.e("LOGIN_DEBUG", "FALLO EL LOGIN CON GOOGLE", error)

                    when (error) {
                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                            Toast.makeText(this, "Token inválido o expirado (posible SHA-1 mal)", Toast.LENGTH_LONG).show()
                        is com.google.firebase.auth.FirebaseAuthException -> {
                            Log.e("LOGIN_DEBUG", "ErrorCode Firebase: ${error.errorCode}")
                            when (error.errorCode) {
                                "ERROR_INVALID_CREDENTIAL" -> Toast.makeText(this, "Credencial inválida - Revisa SHA-1 y webClientId", Toast.LENGTH_LONG).show()
                                "ERROR_OPERATION_NOT_ALLOWED" -> Toast.makeText(this, "Google Sign-In NO está habilitado en Firebase Console", Toast.LENGTH_LONG).show()
                                "ERROR_DEVELOPER_ERROR" -> Toast.makeText(this, "ERROR_DEVELOPER_ERROR - SHA-1 FALTANTE o webClientId mal", Toast.LENGTH_LONG).show()
                                else -> Toast.makeText(this, "Error Firebase: ${error.errorCode}", Toast.LENGTH_LONG).show()
                            }
                        }
                        else -> Toast.makeText(this, "Error desconocido: ${error?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun irDashboard() {
        CoroutineScope(Dispatchers.IO).launch {
            // Limpiar datos locales antes de cargar los del usuario actual
            AppDatabase.getInstance(applicationContext).clearAllUserData()
            runOnUiThread {
                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                finish()
            }
        }
    }
}