package com.example.gastosapp.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gastosapp.Adapters.GastoAgrupadoAdapter
import com.example.gastosapp.Models.Gasto
import com.example.gastosapp.databinding.FragmentGastoBinding
import com.example.gastosapp.viewModels.GastoViewModel
import com.example.gastosapp.viewModels.PresupuestoViewModel

class FragmentGasto : Fragment() {

    private var _binding: FragmentGastoBinding? = null
    private val binding get() = _binding!!

    private val gastoVM: GastoViewModel by activityViewModels()
    private val presupuestoVM: PresupuestoViewModel by activityViewModels()

    private lateinit var adapter: GastoAgrupadoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGastoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observarGastos()
        observarPresupuestos()

        // CONFIGURAR CLICK DEL BOTÓN
        binding.fabAgregarGasto.setOnClickListener {
            abrirDialogoAgregar()
        }

        // También puedes configurar el click en el LinearLayout interno
        val linearLayout = binding.fabAgregarGasto.getChildAt(0)
        linearLayout.setOnClickListener {
            abrirDialogoAgregar()
        }
    }

    private fun setupRecyclerView() {
        adapter = GastoAgrupadoAdapter(
            onEditarClick = { gasto -> abrirDialogoEditar(gasto) },
            onEliminarClick = { gasto -> confirmarEliminar(gasto) }
        )

        binding.recyclerViewGastos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FragmentGasto.adapter
        }
    }

    private fun observarGastos() {
        gastoVM.gastos.observe(viewLifecycleOwner) { gastos ->
            if (gastos.isEmpty()) {
                binding.recyclerViewGastos.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.VISIBLE
            } else {
                binding.recyclerViewGastos.visibility = View.VISIBLE
                binding.emptyStateLayout.visibility = View.GONE
                adapter.submitList(gastos)

                val totalMes = gastos.sumOf { it.monto }
                binding.tvResumenMes.text = "Total del mes: $${String.format("%.2f", totalMes)}"
            }
        }
    }

    private fun observarPresupuestos() {
        presupuestoVM.presupuestos.observe(viewLifecycleOwner) { presupuestos ->
            val categorias = presupuestos.map { it.categoriaNombre }.distinct()
            adapter.updateCategoriasDisponibles(categorias)
            adapter.updatePresupuestos(presupuestos)
        }
    }

    fun abrirDialogoAgregar() {
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
                val presupuesto = presupuestoVM.presupuestos.value
                    ?.firstOrNull { it.categoriaNombre == nuevoGasto.categoriaNombre }

                if (presupuesto != null) {
                    val disponible = presupuesto.cantidad - presupuesto.montoGastado
                    if (nuevoGasto.monto > disponible) {
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Presupuesto insuficiente")
                            .setMessage(
                                "El gasto de \$${String.format("%.2f", nuevoGasto.monto)} supera el saldo disponible " +
                                        "de \$${String.format("%.2f", disponible)} en la categoría \"${nuevoGasto.categoriaNombre}\"."
                            )
                            .setPositiveButton("Entendido", null)
                            .show()
                        return@setOnGastoSaved
                    }
                }

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
                gastoVM.editarGasto(gastoEditado, gasto)
                Toast.makeText(requireContext(), "Gasto actualizado", Toast.LENGTH_SHORT).show()
            }
        }.show(parentFragmentManager, "editar_gasto")
    }

    private fun confirmarEliminar(gasto: Gasto) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar gasto")
            .setMessage("¿Eliminar '${gasto.nombre}' de $${String.format("%.2f", gasto.monto)}?")
            .setPositiveButton("Eliminar") { _, _ ->
                gastoVM.eliminarGasto(gasto)
                Toast.makeText(requireContext(), "Gasto eliminado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}