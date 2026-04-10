package com.example.gastosapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.gastosapp.Fragments.*
import com.example.gastosapp.databinding.ActivityDashboardBinding
import com.example.gastosapp.utils.FirebaseUtils

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        if (!FirebaseUtils.isLoggedIn()) {
            finish()
            return
        }

        if (savedInstanceState == null) {
            replaceFragment(FragmentInicio(), "INICIO")
        }

        binding.bottomNV.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_inicio -> replaceFragment(FragmentInicio(), "INICIO")
                R.id.item_gasto -> replaceFragment(FragmentGasto(), "GASTOS")
                R.id.item_presupuesto -> replaceFragment(FragmentPresupuesto(), "PRESUPUESTOS")
                R.id.item_resumen -> replaceFragment(FragmentResumen(), "RESUMEN")
                R.id.item_perfil -> replaceFragment(FragmentPerfil(), "PERFIL")
                else -> false
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