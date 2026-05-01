package com.example.gastosapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.gastosapp.Fragments.*
import android.widget.ImageView
import android.widget.TextView
import com.example.gastosapp.databinding.ActivityDashboardBinding
import com.example.gastosapp.utils.FirebaseUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.squareup.picasso.Picasso

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        if (!FirebaseUtils.isLoggedIn()) {
            finish()
            return
        }

        configurarDrawer()
        configurarToolbar()
        cargarAvatares()

        if (savedInstanceState == null) {
            replaceFragment(FragmentInicio(), "INICIO")
        }

        binding.bottomNV.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_inicio -> {
                    replaceFragment(FragmentInicio(), "INICIO")
                    true
                }
                R.id.item_gasto -> {
                    replaceFragment(FragmentGasto(), "GASTOS")
                    true
                }
                R.id.item_presupuesto -> {
                    replaceFragment(FragmentPresupuesto(), "PRESUPUESTOS")
                    true
                }
                R.id.item_resumen -> {
                    replaceFragment(FragmentResumen(), "RESUMEN")
                    true
                }
                else -> false
            }
        }
    }

    private fun configurarDrawer() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_perfil -> {
                    replaceFragment(FragmentPerfil(), "PERFIL")
                    true
                }
                R.id.nav_inicio -> {
                    replaceFragment(FragmentInicio(), "INICIO")
                    binding.bottomNV.selectedItemId = R.id.item_inicio
                    true
                }
                R.id.nav_gastos -> {
                    replaceFragment(FragmentGasto(), "GASTOS")
                    binding.bottomNV.selectedItemId = R.id.item_gasto
                    true
                }
                R.id.nav_presupuestos -> {
                    replaceFragment(FragmentPresupuesto(), "PRESUPUESTOS")
                    binding.bottomNV.selectedItemId = R.id.item_presupuesto
                    true
                }
                R.id.nav_resumen -> {
                    replaceFragment(FragmentResumen(), "RESUMEN")
                    binding.bottomNV.selectedItemId = R.id.item_resumen
                    true
                }
                R.id.nav_cerrar_sesion -> {
                    cerrarSesion()
                    true
                }
                else -> false
            }
        }
    }

    private fun configurarToolbar() {
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.imgAvatar.setOnClickListener {
            replaceFragment(FragmentPerfil(), "PERFIL")
        }
    }

    private fun cargarAvatares() {
        val user = FirebaseUtils.auth.currentUser ?: return
        val photoUrl = user.photoUrl?.toString()

        // Avatar del Toolbar
        if (photoUrl != null) {
            Picasso.get()
                .load(photoUrl)
                .placeholder(R.drawable.icon_perfil)
                .into(binding.imgAvatar)
        }

        // Avatar del Navigation Drawer (header)
        val headerView = binding.navigationView.getHeaderView(0)
        val imgNavHeader = headerView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.imgNavHeader)
        val tvNavNombre = headerView.findViewById<TextView>(R.id.tvNavNombre)
        val tvNavCorreo = headerView.findViewById<TextView>(R.id.tvNavCorreo)

        if (photoUrl != null) {
            Picasso.get()
                .load(photoUrl)
                .placeholder(R.drawable.icon_perfil)
                .into(imgNavHeader)
        }

        // Cargar nombre y correo en el header
        tvNavNombre.text = FirebaseUtils.displayName() ?: "Usuario"
        tvNavCorreo.text = user.email ?: "Sin correo"
    }
    private fun replaceFragment(fragment: Fragment, titulo: String) {
        binding.tvTitulo.text = titulo

        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentoFl.id, fragment)
            .setReorderingAllowed(true)
            .commit()
    }

    private fun cerrarSesion() {
        FirebaseUtils.auth.signOut()
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()

        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}