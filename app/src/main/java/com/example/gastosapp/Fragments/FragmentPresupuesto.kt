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
import com.example.gastosapp.Adapters.PresupuestoAdapter
import com.example.gastosapp.Models.Presupuesto
import com.example.gastosapp.databinding.FragmentPresupuestoBinding
import com.example.gastosapp.Utils.FirebaseUtils
import com.example.gastosapp.ViewModels.PresupuestoViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class FragmentPresupuesto : Fragment() {

    private var _binding: FragmentPresupuestoBinding? = null
    private val binding get() = _binding!!

    private val vm: PresupuestoViewModel by activityViewModels()
    private lateinit var adapter: PresupuestoAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPresupuestoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observarPresupuestos()

        binding.fabAgregarPresupuesto.setOnClickListener {
            abrirAgregar()
        }
    }

    private fun setupRecyclerView() {
        adapter = PresupuestoAdapter(
            onEditarClick  = { presupuesto, esAgotado -> abrirEdicion(presupuesto, esAgotado) },
            onEliminarClick = { presupuesto -> confirmarEliminar(presupuesto) }
        )

        binding.recyclerViewPresupuestos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FragmentPresupuesto.adapter
        }
    }

    private fun observarPresupuestos() {
        vm.presupuestos.observe(viewLifecycleOwner) { presupuestos ->
            if (presupuestos.isEmpty()) {
                binding.recyclerViewPresupuestos.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.VISIBLE
            } else {
                binding.recyclerViewPresupuestos.visibility = View.VISIBLE
                binding.emptyStateLayout.visibility = View.GONE
                adapter.submitList(presupuestos)

                val total = presupuestos.sumOf { presupuesto -> presupuesto.cantidad }
                binding.tvTotalPresupuestado.text = "Total presupuestado: $${String.format("%.2f", total)}"
            }
        }
    }

    fun abrirAgregar() {
        FragmentAgregarPresupuesto().apply {
            arguments = Bundle().apply {
                putStringArrayList("categorias_ocupadas", ArrayList(obtenerCategoriasOcupadas()))
            }
            setOnPresupuestoSaved { vm.agregarPresupuesto(it) }
        }.show(parentFragmentManager, "agregar_presupuesto")
    }

    private fun abrirEdicion(p: Presupuesto, esAgotado: Boolean = false) {
        FragmentAgregarPresupuesto().apply {
            arguments = Bundle().apply {
                putStringArrayList("categorias_ocupadas", ArrayList(obtenerCategoriasOcupadas(p.id)))
                // Si está agotado, bloqueamos el spinner de categoría
                putBoolean("bloquear_categoria", esAgotado)
            }
            editarPresupuesto(p) { vm.editarPresupuesto(it) }
        }.show(parentFragmentManager, "editar_presupuesto")
    }

    private fun confirmarEliminar(p: Presupuesto) {
        val uid = FirebaseUtils.uid() ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("gastos").document(uid).collection("mis_gastos")
            .whereEqualTo("categoriaNombre", p.categoriaNombre)
            .get()
            .addOnSuccessListener { snapshot ->
                val cantidadGastos = snapshot.size()

                // Mensaje diferente según si tiene o no gastos asociados
                val mensaje = if (cantidadGastos > 0)
                    "Este presupuesto tiene $cantidadGastos gasto(s) asociado(s) en '${p.categoriaNombre}'.\n\nTodos esos gastos también serán eliminados. ¿Desea continuar?"
                else
                    "¿Eliminar el presupuesto de '${p.categoriaNombre}'?\n\nEsta acción no se puede deshacer."

                AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar presupuesto")
                    .setMessage(mensaje)
                    .setPositiveButton("Eliminar") { _, _ ->
                        // El ViewModel se encarga de la eliminación en cascada
                        vm.eliminarPresupuesto(p)
                        val msg = if (cantidadGastos > 0)
                            "Presupuesto y $cantidadGastos gasto(s) eliminados"
                        else
                            "Presupuesto eliminado"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .addOnFailureListener {
                // Si falla la consulta previa, igual mostrar confirmación
                AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar presupuesto")
                    .setMessage("¿Eliminar el presupuesto de '${p.categoriaNombre}' y sus gastos asociados?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        vm.eliminarPresupuesto(p)
                        Toast.makeText(requireContext(), "Presupuesto eliminado", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
    }

    private fun obtenerCategoriasOcupadas(excluirId: String? = null): List<String> {
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return vm.presupuestos.value?.filter { p ->
            p.id != excluirId && (p.fechaFinal.after(hoy.time) || sameDay(p.fechaFinal, hoy.time))
        }?.map { presupuesto -> presupuesto.categoriaNombre } ?: emptyList()
    }

    private fun sameDay(d1: Date, d2: Date): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(d1) == fmt.format(d2)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}