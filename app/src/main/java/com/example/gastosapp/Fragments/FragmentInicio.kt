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
import com.example.gastosapp.DashboardActivity
import com.example.gastosapp.Models.Categoria
import com.example.gastosapp.Models.Gasto
import com.example.gastosapp.R
import com.example.gastosapp.databinding.FragmentInicioBinding
import com.example.gastosapp.Utils.FirebaseUtils
import com.example.gastosapp.ViewModels.GastoViewModel
import java.text.SimpleDateFormat
import java.util.*

class FragmentInicio : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private val gastoVM: GastoViewModel by activityViewModels()

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        actualizarSaludoYNombre()
        configurarBotonesNavegacion()
        observarGastos()
    }

    private fun configurarBotonesNavegacion() {
        binding.cardGastos.setOnClickListener {
            (requireActivity() as DashboardActivity).navegarAFragment(FragmentGasto(), "GASTOS")
        }

        binding.cardPresupuestos.setOnClickListener {
            (requireActivity() as DashboardActivity).navegarAFragment(FragmentPresupuesto(), "PRESUPUESTOS")
        }
    }
    private fun actualizarSaludoYNombre() {
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val saludo = when (hora) {
            in 5..11 -> "¡Buenos días!"
            in 12..18 -> "¡Buenas tardes!"
            else -> "¡Buenas noches!"
        }
        binding.tvSaludo.text = saludo

        // Usar el nombre del usuario de Firebase Auth directamente
        val nombre = FirebaseUtils.displayName()
        if (!nombre.isNullOrBlank() && nombre != "Sin nombre") {
            binding.tvNombreUsuario.text = nombre
        } else {
            val correo = FirebaseUtils.email()
            binding.tvNombreUsuario.text = correo?.substringBefore("@") ?: "Usuario"
        }
    }

    private fun observarGastos() {
        // Solo observamos gastos para mostrar los recientes
        gastoVM.gastos.observe(viewLifecycleOwner) { gastos ->
            actualizarGastosRecientes(gastos)
        }
    }

    private fun actualizarGastosRecientes(gastos: List<Gasto>) {
        binding.containerGastosRecientes.removeAllViews()

        if (gastos.isEmpty()) {
            binding.containerGastosRecientes.addView(crearTextoVacio("No hay gastos recientes"))
            return
        }

        gastos.sortedByDescending { it.fecha }
            .take(3)
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

        val colorCategoria = obtenerColorPorCategoria(gasto.categoriaNombre)
        view.findViewById<TextView>(R.id.tvCategoriaGastoReciente).setTextColor(colorCategoria)

        return view
    }

    private fun obtenerColorPorCategoria(catNombre: String): Int {
        val cat = Categoria.fromNombre(catNombre)
        return when (cat) {
            Categoria.ALIMENTACION -> 0xFFE57373.toInt()
            Categoria.TRANSPORTE -> 0xFF7986CB.toInt()
            Categoria.ENTRETENIMIENTO -> 0xFFFFB74D.toInt()
            Categoria.SALUD -> 0xFF81C784.toInt()
            Categoria.EDUCACION -> 0xFF9575CD.toInt()
            Categoria.COMPRAS -> 0xFFFF8A65.toInt()
            Categoria.HOGAR -> 0xFFA1887F.toInt()
            Categoria.OTROS -> 0xFF90A4AE.toInt()
        }
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