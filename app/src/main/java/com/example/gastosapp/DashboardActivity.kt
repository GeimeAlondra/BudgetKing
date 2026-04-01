package com.example.gastosapp

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.gastosapp.Fragments.FragmentGasto
import com.example.gastosapp.Fragments.FragmentInicio
import com.example.gastosapp.Fragments.FragmentPerfil
import com.example.gastosapp.Fragments.FragmentPresupuesto
import com.example.gastosapp.Fragments.FragmentResumen
import com.example.gastosapp.databinding.ActivityDashboardBinding
import com.example.gastosapp.utils.FirebaseUtils
import com.example.gastosapp.viewModels.GastoViewModel
import com.example.gastosapp.viewModels.PresupuestoViewModel
import com.example.gastosapp.viewModels.ResumenViewModel

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    // viewModels() en la Activity es la misma instancia que activityViewModels() en los Fragments
    private val gastoVM: GastoViewModel by viewModels()
    private val presupuestoVM: PresupuestoViewModel by viewModels()
    private val resumenVM: ResumenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (!FirebaseUtils.isLoggedIn()) {
            finish()
            return
        }

        // Cada vez que Firestore notifica un cambio en gastos, recalculamos el resumen
        gastoVM.onGastosActualizados = { totalGastado ->
            val montoInicial = presupuestoVM.presupuestos.value?.sumOf { it.cantidad } ?: 0.0
            resumenVM.recalcularYGuardar(totalGastado, montoInicial)
        }

        // Cada vez que Firestore notifica un cambio en presupuestos, recalculamos el resumen
        presupuestoVM.onPresupuestosActualizados = { montoInicial ->
            val totalGastado = gastoVM.gastos.value?.sumOf { it.monto } ?: 0.0
            resumenVM.recalcularYGuardar(totalGastado, montoInicial)
        }

        if (savedInstanceState == null) {
            replaceFragment(FragmentInicio(), "Inicio")
        }

        binding.bottomNV.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_inicio -> replaceFragment(FragmentInicio(), "Inicio")
                R.id.item_gasto -> replaceFragment(FragmentGasto(), "Gastos")
                R.id.item_presupuesto -> replaceFragment(FragmentPresupuesto(), "Presupuestos")
                R.id.item_resumen -> replaceFragment(FragmentResumen(), "Resumen")
                R.id.item_perfil -> replaceFragment(FragmentPerfil(), "Perfil")
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment, titulo: String) {
        binding.tvTitulo.text = titulo
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentoFl.id, fragment)
            .setReorderingAllowed(true)
            .commit()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}