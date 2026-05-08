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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.gastosapp.Utils.FirebaseUtils
import com.example.gastosapp.MainActivity
import com.example.gastosapp.R
import com.example.gastosapp.ViewModels.GastoViewModel
import com.example.gastosapp.ViewModels.PresupuestoViewModel
import com.example.gastosapp.databinding.FragmentPerfilBinding
import com.example.gastosapp.db.AppDatabase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import java.util.Date

class FragmentPerfil : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val gastoVM: GastoViewModel by activityViewModels()
    private val presupuestoVM: PresupuestoViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mostrarDatosUsuario()
        observarEstadisticas()

        binding.btnCerrarSesion.setOnClickListener { cerrarSesion() }
        binding.btnEditarNombre.setOnClickListener { mostrarDialogoEditarNombre() }
    }

    private fun mostrarDatosUsuario() {
        val user = FirebaseUtils.auth.currentUser ?: return

        FirebaseUtils.db.collection("usuarios").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val nombre = doc.getString("nombre") ?: user.displayName ?: "Sin nombre"
                binding.pNombre.text = nombre
            }
            .addOnFailureListener {
                binding.pNombre.text = user.displayName ?: "Sin nombre"
            }

        binding.perfilCorreo.text = user.email ?: "Sin correo"

        user.photoUrl?.let { uri ->
            Picasso.get().load(uri).placeholder(R.drawable.icon_perfil).into(binding.imgPerfil)
        }
    }

    private fun observarEstadisticas() {
        // Total gastado y número de gastos
        gastoVM.gastos.observe(viewLifecycleOwner) { gastos ->
            val total = gastos.sumOf { it.monto }
            binding.tvStatTotalGastado.text = "$${String.format("%.2f", total)}"
            binding.tvStatNumGastos.text = gastos.size.toString()

            // Categoría con más gasto
            val categoriaTop = gastos
                .groupBy { it.categoriaNombre }
                .mapValues { entry -> entry.value.sumOf { it.monto } }
                .maxByOrNull { it.value }
                ?.key
            binding.tvStatCategoriaTop.text = categoriaTop ?: "—"
        }

        // Presupuestos activos (no agotados y no expirados)
        presupuestoVM.presupuestos.observe(viewLifecycleOwner) { presupuestos ->
            val activos = presupuestos.count { p ->
                p.montoGastado < p.cantidad && !p.fechaFinal.before(Date())
            }
            binding.tvStatPresupuestosActivos.text = activos.toString()
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
        lifecycleScope.launch {
            // Borrar Room primero, antes de que cambie el uid
            AppDatabase.getInstance(requireContext()).clearAllUserData()

            // Cerrar sesión de Firebase y Google
            FirebaseUtils.auth.signOut()
            GoogleSignIn.getClient(requireActivity(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()

            startActivity(Intent(requireActivity(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}