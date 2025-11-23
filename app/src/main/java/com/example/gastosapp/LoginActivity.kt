package com.example.gastosapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.gastosapp.databinding.ActivityLoginBinding
import com.example.gastosapp.utils.FirebaseUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

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
            when (e.statusCode) {
                10 -> Toast.makeText(this, "DEVELOPER ERROR - SHA-1 NO REGISTRADO EN FIREBASE", Toast.LENGTH_LONG).show()
                12500 -> Toast.makeText(this, "Google Sign-In cancelado o error interno", Toast.LENGTH_LONG).show()
                7 -> Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_LONG).show()
                else -> Toast.makeText(this, "Error Google ApiException: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("LOGIN_DEBUG", "Error desconocido en launcher", e)
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

            FirebaseUtils.auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { irDashboard() }
                .addOnFailureListener {
                    Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnLoginGoogle.setOnClickListener { googleLauncher.launch(googleClient.signInIntent) }
        binding.sinCuenta.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
    }

    private fun setupGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)
    }

    private fun authWithGoogle(idToken: String) {
        Log.d("LOGIN_DEBUG", "ID Token recibido: $idToken")  // ← Para ver si llega

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseUtils.auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LOGIN_DEBUG", "¡LOGIN CON GOOGLE EXITOSO! UID: ${FirebaseUtils.auth.currentUser?.uid}")
                    irDashboard()
                } else {
                    // ← AQUÍ ESTÁ LA MAGIA: TE DICE EL CÓDIGO EXACTO DEL ERROR
                    val error = task.exception
                    Log.e("LOGIN_DEBUG", "FALLO EL LOGIN CON GOOGLE", error)

                    when (error) {
                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(this, "Token inválido o expirado (posible SHA-1 mal)", Toast.LENGTH_LONG).show()
                        }
                        is com.google.firebase.auth.FirebaseAuthException -> {
                            val errorCode = error.errorCode
                            Log.e("LOGIN_DEBUG", "ErrorCode Firebase: $errorCode")

                            when (errorCode) {
                                "ERROR_INVALID_CREDENTIAL" -> Toast.makeText(this, "Credencial inválida - Revisa SHA-1 y webClientId", Toast.LENGTH_LONG).show()
                                "ERROR_OPERATION_NOT_ALLOWED" -> Toast.makeText(this, "Google Sign-In NO está habilitado en Firebase Console", Toast.LENGTH_LONG).show()
                                "ERROR_DEVELOPER_ERROR" -> Toast.makeText(this, "ERROR_DEVELOPER_ERROR - SHA-1 FALTANTE o webClientId mal", Toast.LENGTH_LONG).show()
                                else -> Toast.makeText(this, "Error Firebase: $errorCode", Toast.LENGTH_LONG).show()
                            }
                        }
                        else -> Toast.makeText(this, "Error desconocido: ${error?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun irDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}