package com.example.gastosapp.Fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.gastosapp.Models.Categoria
import com.example.gastosapp.Models.Gasto
import com.example.gastosapp.Models.Presupuesto
import com.example.gastosapp.R
import com.example.gastosapp.databinding.FragmentInicioBinding
import com.example.gastosapp.utils.FirebaseUtils
import com.example.gastosapp.viewModels.GastoViewModel
import com.example.gastosapp.viewModels.PresupuestoViewModel
import java.text.SimpleDateFormat
import java.util.*

class FragmentInicio : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private val gastoVM: GastoViewModel by activityViewModels()
    private val presupuestoVM: PresupuestoViewModel by activityViewModels()

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        actualizarSaludoYNombre()
        observarDatos()
    }

    private fun actualizarSaludoYNombre() {
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val saludo = when (hora) {
            in 5..11 -> "¡Buenos días!"
            in 12..18 -> "¡Buenas tardes!"
            else -> "¡Buenas noches!"
        }
        binding.tvSaludo.text = saludo

        val nombre = FirebaseUtils.displayName()
            ?: FirebaseUtils.email()?.substringBefore("@")
            ?: "Usuario"
        binding.tvNombreUsuario.text = nombre
    }

    private fun observarDatos() {
        presupuestoVM.presupuestos.observe(viewLifecycleOwner) { actualizarResumen(it) }
        gastoVM.gastos.observe(viewLifecycleOwner) { actualizarGastosRecientes(it) }
    }

    private fun actualizarResumen(presupuestos: List<Presupuesto>) {
        val total = presupuestos.sumOf { it.cantidad }
        val gastado = presupuestos.sumOf { it.montoGastado }
        val disponible = total - gastado

        binding.tvPresupuestoTotalInicio.text = String.format("$%.2f", total)
        binding.tvGastadoTotalInicio.text = String.format("$%.2f", gastado)
        binding.tvSaldoDisponibleInicio.text = String.format("$%.2f", disponible)
    }

    private fun actualizarGastosRecientes(gastos: List<Gasto>) {
        binding.containerGastosRecientes.removeAllViews()

        if (gastos.isEmpty()) {
            binding.containerGastosRecientes.addView(crearTextoVacio("No hay gastos recientes"))
            return
        }

        gastos.sortedByDescending { it.fecha }
            .take(5)
            .forEach { binding.containerGastosRecientes.addView(crearItemGasto(it)) }
    }

    private fun crearItemGasto(gasto: Gasto): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_gasto_reciente, binding.containerGastosRecientes, false)

        view.findViewById<TextView>(R.id.tvNombreGastoReciente).text = gasto.nombre
        view.findViewById<TextView>(R.id.tvMontoGastoReciente).apply {
            text = String.format("-$%.2f", gasto.monto)
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
        }
        view.findViewById<TextView>(R.id.tvCategoriaGastoReciente).text = gasto.categoriaNombre
        view.findViewById<TextView>(R.id.tvFechaGastoReciente).text = sdf.format(gasto.fecha)

        return view
    }

    private fun crearTextoVacio(mensaje: String) = TextView(requireContext()).apply {
        text = mensaje
        textSize = 16f
        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        gravity = Gravity.CENTER
        setPadding(0, 80, 0, 80)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}