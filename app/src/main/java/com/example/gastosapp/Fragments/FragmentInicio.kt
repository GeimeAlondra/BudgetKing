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
import com.example.gastosapp.Utils.FirebaseUtils
import com.example.gastosapp.ViewModels.GastoViewModel
import com.example.gastosapp.ViewModels.PresupuestoViewModel
import java.text.SimpleDateFormat
import java.util.*

class FragmentInicio : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private val gastoVM: GastoViewModel by activityViewModels()
    private val presupuestoVM: PresupuestoViewModel by activityViewModels()

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Variables para almacenar los datos actuales
    private var gastosActuales = emptyList<Gasto>()
    private var presupuestosActuales = emptyList<Presupuesto>()

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

        // SOLUCIÓN: Usar el nombre del usuario de Firebase Auth directamente
        val nombre = FirebaseUtils.displayName()
        if (!nombre.isNullOrBlank() && nombre != "Sin nombre") {
            // Si tiene nombre en Firebase Auth
            binding.tvNombreUsuario.text = nombre
        } else {
            // Si no, usar la parte del correo antes del @
            val correo = FirebaseUtils.email()
            binding.tvNombreUsuario.text = correo?.substringBefore("@") ?: "Usuario"
        }
    }
    private fun observarDatos() {
        // Observar presupuestos
        presupuestoVM.presupuestos.observe(viewLifecycleOwner) { presupuestos ->
            presupuestosActuales = presupuestos
            actualizarResumen()
        }

        // Observar gastos
        gastoVM.gastos.observe(viewLifecycleOwner) { gastos ->
            gastosActuales = gastos
            actualizarGastosRecientes(gastos)
            actualizarResumen()  // También actualizar resumen cuando cambian los gastos
        }
    }

    private fun actualizarResumen() {
        // Total de presupuesto asignado
        val presupuestoTotal = presupuestosActuales.sumOf { it.cantidad }

        // Total gastado (suma de todos los gastos reales)
        val gastadoTotal = gastosActuales.sumOf { it.monto }

        // Saldo disponible
        val saldoDisponible = presupuestoTotal - gastadoTotal

        // Actualizar UI
        binding.tvPresupuestoTotalInicio.text = String.format("$%.2f", presupuestoTotal)
        binding.tvGastadoTotalInicio.text = String.format("$%.2f", gastadoTotal)
        binding.tvSaldoDisponibleInicio.text = String.format("$%.2f", saldoDisponible)

        // Cambiar color del saldo según sea positivo o negativo
        if (saldoDisponible < 0) {
            binding.tvSaldoDisponibleInicio.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
        } else {
            binding.tvSaldoDisponibleInicio.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
        }
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

        // Agregar ícono o color según categoría (opcional)
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