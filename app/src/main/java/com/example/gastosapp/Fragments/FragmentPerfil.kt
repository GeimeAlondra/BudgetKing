package com.example.gastosapp.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.example.gastosapp.utils.FirebaseUtils
import com.example.gastosapp.LoginActivity
import com.example.gastosapp.MainActivity
import com.example.gastosapp.R
import com.example.gastosapp.databinding.FragmentPerfilBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.card.MaterialCardView
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
    }

    private fun mostrarDatosUsuario() {
        val user = FirebaseUtils.auth.currentUser ?: return

        binding.pNombre.text = FirebaseUtils.displayName() ?: "Sin nombre"
        binding.pCorreo.text = user.email ?: "Sin correo"

        user.photoUrl?.let { uri ->
            Picasso.get().load(uri).placeholder(R.drawable.icon_perfil).into(binding.imgPerfil)
        }
    }

    private fun cerrarSesion() {
        FirebaseUtils.auth.signOut()

        // Cerrar sesión de Google también
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