package com.example.gastosapp.Fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.gastosapp.Models.Categoria
import com.example.gastosapp.Models.Gasto
import com.example.gastosapp.R
import com.example.gastosapp.databinding.FragmentGastoBinding
import com.example.gastosapp.viewModels.GastoViewModel
import com.example.gastosapp.viewModels.PresupuestoViewModel
import java.text.SimpleDateFormat
import java.util.*

class FragmentGasto : Fragment() {

    private var _binding: FragmentGastoBinding? = null
    private val binding get() = _binding!!

    private val gastoVM: GastoViewModel by activityViewModels()
    private val presupuestoVM: PresupuestoViewModel by activityViewModels()

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGastoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gastoVM.gastos.observe(viewLifecycleOwner) { gastos ->
            actualizarLista(gastos)
        }

        binding.agregarGasto.setOnClickListener {
            binding.agregarGasto.playAnimation()
            abrirDialogoAgregar()
        }
    }

    private fun abrirDialogoAgregar() {
        val categoriasValidas = presupuestoVM.presupuestos.value
            ?.filter { it.cantidad > it.montoGastado }
            ?.map { it.categoriaNombre }
            ?.distinct()
            ?: emptyList()

        FragmentAgregarGasto().apply {
            arguments = Bundle().apply {
                putStringArrayList("categorias_validas", ArrayList(categoriasValidas))
            }
            setOnGastoSaved { nuevoGasto ->
                gastoVM.agregarGasto(nuevoGasto)
                Toast.makeText(requireContext(), "Gasto agregado", Toast.LENGTH_SHORT).show()
            }
        }.show(parentFragmentManager, "agregar_gasto")
    }

    private fun abrirDialogoEditar(gasto: Gasto) {
        val categoriasValidas = presupuestoVM.presupuestos.value
            ?.filter { it.cantidad > it.montoGastado }
            ?.map { it.categoriaNombre }
            ?.distinct()
            ?: emptyList()

        FragmentAgregarGasto().apply {
            arguments = Bundle().apply {
                putStringArrayList("categorias_validas", ArrayList(categoriasValidas))
            }
            editarGasto(gasto) { gastoEditado ->
                // ← AHORA PASAMOS LOS DOS PARÁMETROS
                gastoVM.editarGasto(gastoEditado, gasto)  // gastoOriginal = gasto actual
                Toast.makeText(requireContext(), "Gasto actualizado", Toast.LENGTH_SHORT).show()
            }
        }.show(parentFragmentManager, "editar_gasto")
    }

    private fun confirmarEliminar(gasto: Gasto) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar gasto")
            .setMessage("¿Eliminar '${gasto.nombre}' de $${gasto.monto}?")
            .setPositiveButton("Eliminar") { _, _ ->
                gastoVM.eliminarGasto(gasto)
                Toast.makeText(requireContext(), "Gasto eliminado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarLista(gastos: List<Gasto>) {
        binding.containerGastos.removeAllViews()

        if (gastos.isEmpty()) {
            binding.containerGastos.addView(TextView(requireContext()).apply {
                text = "No hay gastos registrados"
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, 120, 0, 120)
                setTextColor(requireContext().getColor(android.R.color.darker_gray))
            })
            return
        }

        gastos.forEach { gasto ->
            binding.containerGastos.addView(crearItemGasto(gasto))
        }
    }

    private fun crearItemGasto(gasto: Gasto): View {
        val item = layoutInflater.inflate(R.layout.item_gasto, binding.containerGastos, false)

        item.findViewById<TextView>(R.id.tvNombreGasto).text = gasto.nombre
        item.findViewById<TextView>(R.id.tvCantidadGasto).text = "-$${String.format("%.2f", gasto.monto)}"
        item.findViewById<TextView>(R.id.tvDescripcion).text = gasto.descripcion
        item.findViewById<TextView>(R.id.tvCategoria).text = gasto.categoriaNombre
        item.findViewById<TextView>(R.id.tvFechaGasto).text = sdf.format(gasto.fecha)

        item.findViewById<View>(R.id.btnEditarGasto).setOnClickListener {
            abrirDialogoEditar(gasto)
        }

        item.findViewById<View>(R.id.btnEliminarGasto).setOnClickListener {
            confirmarEliminar(gasto)
        }

        return item
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}