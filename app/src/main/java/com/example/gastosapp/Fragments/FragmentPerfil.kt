package com.example.gastosapp.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.gastosapp.Utils.FirebaseUtils
import com.example.gastosapp.MainActivity
import com.example.gastosapp.R
import com.example.gastosapp.databinding.FragmentPerfilBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.squareup.picasso.Picasso

class FragmentPerfil : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mostrarDatosUsuario()
        binding.btnCerrarSesion.setOnClickListener { cerrarSesion() }

        // NC-009/C2: Permitir editar el nombre del usuario
        binding.btnEditarNombre.setOnClickListener { mostrarDialogoEditarNombre() }
    }

    private fun mostrarDatosUsuario() {
        val user = FirebaseUtils.auth.currentUser ?: return
        val uid = user.uid

        // Cargar nombre desde Firestore (fuente de verdad)
        FirebaseUtils.db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val nombre = doc.getString("nombre") ?: user.displayName ?: "Sin nombre"
                binding.pNombre.text = nombre
            }
            .addOnFailureListener {
                binding.pNombre.text = user.displayName ?: "Sin nombre"
            }

        binding.pCorreo.text = user.email ?: "Sin correo"

        user.photoUrl?.let { uri ->
            Picasso.get().load(uri).placeholder(R.drawable.icon_perfil).into(binding.imgPerfil)
        }
    }

    private fun mostrarDialogoEditarNombre() {
        val uid = FirebaseUtils.uid() ?: return
        val input = EditText(requireContext()).apply {
            hint = "Nuevo nombre"
            setText(binding.pNombre.text)
            setPadding(48, 24, 48, 24)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Editar nombre")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNombre = input.text.toString().trim()

                // Validar formato antes de guardar
                when {
                    nuevoNombre.isEmpty() -> {
                        Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    nuevoNombre.length < 2 -> {
                        Toast.makeText(requireContext(), "El nombre debe tener al menos 2 caracteres", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    nuevoNombre.length > 50 -> {
                        Toast.makeText(requireContext(), "El nombre no puede superar 50 caracteres", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    !nuevoNombre.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+$")) -> {
                        Toast.makeText(requireContext(), "Solo se permiten letras y espacios", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }

                FirebaseUtils.db.collection("usuarios").document(uid)
                    .update("nombre", nuevoNombre)
                    .addOnSuccessListener {
                        binding.pNombre.text = nuevoNombre
                        Toast.makeText(requireContext(), "Nombre actualizado", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error al actualizar: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cerrarSesion() {
        FirebaseUtils.auth.signOut()
        GoogleSignIn.getClient(requireActivity(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()

        startActivity(Intent(requireActivity(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}