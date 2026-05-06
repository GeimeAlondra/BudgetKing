package com.example.gastosapp.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gastosapp.Adapters.GastoAgrupadoAdapter
import com.example.gastosapp.Models.Gasto
import com.example.gastosapp.databinding.FragmentGastoBinding
import com.example.gastosapp.ViewModels.GastoViewModel
import com.example.gastosapp.ViewModels.PresupuestoViewModel

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

        binding.fabAgregarGasto.setOnClickListener {
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

                val totalMes = gastos.sumOf { gasto -> gasto.monto }
                binding.tvTotalGrande.text = String.format("$%.2f", totalMes)
            }
        }
    }
    private fun observarPresupuestos() {
        presupuestoVM.presupuestos.observe(viewLifecycleOwner) { presupuestos ->
            val categorias = presupuestos.map { presupuesto -> presupuesto.categoriaNombre }.distinct()
            adapter.updateCategoriasDisponibles(categorias)
            adapter.updatePresupuestos(presupuestos)
        }
    }

    fun abrirDialogoAgregar() {
        val presupuestosActivos = presupuestoVM.presupuestos.value ?: emptyList()

        // Bloquear si no hay presupuestos creados
        if (presupuestosActivos.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Sin presupuestos")
                .setMessage("Debes crear al menos un presupuesto antes de registrar un gasto.")
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        val categoriasValidas = presupuestosActivos
            .filter { presupuesto -> presupuesto.cantidad > presupuesto.montoGastado }
            .map { presupuesto -> presupuesto.categoriaNombre }
            .distinct()

        // Bloquear si todos los presupuestos están agotados
        if (categoriasValidas.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Presupuestos agotados")
                .setMessage("Todos tus presupuestos activos están agotados. Edita o crea un nuevo presupuesto para continuar.")
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        FragmentAgregarGasto().apply {
            arguments = Bundle().apply {
                putStringArrayList("categorias_validas", ArrayList(categoriasValidas))
            }
            setOnGastoSaved { nuevoGasto ->
                val presupuesto = presupuestosActivos
                    .firstOrNull { p -> p.categoriaNombre == nuevoGasto.categoriaNombre }

                if (presupuesto != null) {
                    val disponible = presupuesto.cantidad - presupuesto.montoGastado
                    if (nuevoGasto.monto > disponible) {
                        AlertDialog.Builder(requireContext())
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
            ?.filter { presupuesto -> presupuesto.cantidad > presupuesto.montoGastado }
            ?.map { presupuesto -> presupuesto.categoriaNombre }
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
        AlertDialog.Builder(requireContext())
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