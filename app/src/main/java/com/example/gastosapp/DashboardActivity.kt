package com.example.gastosapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.gastosapp.Fragments.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.example.gastosapp.databinding.ActivityDashboardBinding
import com.example.gastosapp.Utils.FirebaseUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.squareup.picasso.Picasso
import com.example.gastosapp.ViewModels.GastoViewModel
import com.example.gastosapp.ViewModels.PresupuestoViewModel
import com.example.gastosapp.ViewModels.ResumenViewModel

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    private val gastoVM: GastoViewModel by viewModels()
    private val presupuestoVM: PresupuestoViewModel by viewModels()
    private val resumenVM: ResumenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        if (!FirebaseUtils.isLoggedIn()) {
            finish()
            return
        }

        configurarDrawer()
        configurarToolbar()
        cargarAvatares()

        gastoVM.onGastosActualizados = { totalGastado ->
            val montoInicial = presupuestoVM.presupuestos.value?.sumOf { presupuesto -> presupuesto.cantidad } ?: 0.0
            resumenVM.recalcularYGuardar(totalGastado, montoInicial)
        }

        presupuestoVM.onPresupuestosActualizados = { montoInicial ->
            val totalGastado = gastoVM.gastos.value?.sumOf { gasto -> gasto.monto } ?: 0.0
            resumenVM.recalcularYGuardar(totalGastado, montoInicial)
        }

        if (savedInstanceState == null) {
            replaceFragment(FragmentInicio(), "INICIO")
        }

        supportFragmentManager.addOnBackStackChangedListener {
            actualizarTituloYBottomNav()
        }

        binding.bottomNV.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_inicio -> {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_from_left,
                            R.anim.slide_out_to_right
                        )
                        .replace(binding.fragmentoFl.id, FragmentInicio())
                        .commit()
                    binding.tvTitulo.text = "INICIO"
                    true
                }
                R.id.item_resumen -> {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                        .replace(binding.fragmentoFl.id, FragmentResumen())
                        .commit()
                    binding.tvTitulo.text = "RESUMEN"
                    true
                }
                else -> false
            }
        }
    }

    fun navegarAFragment(fragment: Fragment, titulo: String) {
        binding.tvTitulo.text = titulo

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(binding.fragmentoFl.id, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
    }

    private fun replaceFragment(fragment: Fragment, titulo: String) {
        binding.tvTitulo.text = titulo

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(binding.fragmentoFl.id, fragment)
            .setReorderingAllowed(true)
            .commit()
    }

    private fun configurarDrawer() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_perfil -> {
                    navegarAFragment(FragmentPerfil(), "PERFIL")
                    true
                }
                R.id.nav_inicio -> {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_from_left,
                            R.anim.slide_out_to_right
                        )
                        .replace(binding.fragmentoFl.id, FragmentInicio())
                        .commit()
                    binding.tvTitulo.text = "INICIO"
                    binding.bottomNV.selectedItemId = R.id.item_inicio
                    true
                }
                R.id.nav_gastos -> {
                    navegarAFragment(FragmentGasto(), "GASTOS")
                    true
                }
                R.id.nav_presupuestos -> {
                    navegarAFragment(FragmentPresupuesto(), "PRESUPUESTOS")
                    true
                }
                R.id.nav_resumen -> {
                    // Usar exactamente la misma lógica que el Bottom Navigation
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                        .replace(binding.fragmentoFl.id, FragmentResumen())
                        .commit()
                    binding.tvTitulo.text = "RESUMEN"
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
            navegarAFragment(FragmentPerfil(), "PERFIL")
        }
    }

    private fun cargarAvatares() {
        val user = FirebaseUtils.auth.currentUser ?: return
        val photoUrl = user.photoUrl?.toString()

        if (photoUrl != null) {
            Picasso.get()
                .load(photoUrl)
                .placeholder(R.drawable.icon_perfil)
                .into(binding.imgAvatar)
        }

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

        tvNavNombre.text = FirebaseUtils.displayName() ?: "Usuario"
        tvNavCorreo.text = user.email ?: "Sin correo"
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
            val currentFragment = supportFragmentManager.findFragmentById(binding.fragmentoFl.id)
            if (currentFragment is FragmentResumen) {
                replaceFragment(FragmentInicio(), "INICIO")
                binding.bottomNV.selectedItemId = R.id.item_inicio
            } else {
                super.onBackPressed()
            }
        }
    }

    private fun actualizarTituloYBottomNav() {
        val currentFragment = supportFragmentManager.findFragmentById(binding.fragmentoFl.id)
        when (currentFragment) {
            is FragmentInicio -> {
                binding.tvTitulo.text = "INICIO"
                binding.bottomNV.selectedItemId = R.id.item_inicio
            }
            is FragmentGasto -> {
                binding.tvTitulo.text = "GASTOS"
            }
            is FragmentPresupuesto -> {
                binding.tvTitulo.text = "PRESUPUESTOS"
            }
            is FragmentResumen -> {
                binding.tvTitulo.text = "RESUMEN"
                binding.bottomNV.selectedItemId = R.id.item_resumen
            }
            is FragmentPerfil -> {
                binding.tvTitulo.text = "PERFIL"
            }
        }
    }
}