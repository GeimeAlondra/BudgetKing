package com.example.gastosapp.Fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.gastosapp.Models.Categoria
import com.example.gastosapp.Models.Presupuesto
import com.example.gastosapp.R
import com.example.gastosapp.databinding.FragmentPresupuestoBinding
import com.example.gastosapp.viewModels.PresupuestoViewModel
import java.text.SimpleDateFormat
import java.util.*

class FragmentPresupuesto : Fragment() {

    private var _binding: FragmentPresupuestoBinding? = null
    private val binding get() = _binding!!

    private val vm: PresupuestoViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPresupuestoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm.presupuestos.observe(viewLifecycleOwner) { actualizarUI(it) }

        binding.agregarPresupuesto.setOnClickListener {
            binding.agregarPresupuesto.playAnimation()
            abrirAgregar()
        }
    }

    private fun abrirAgregar() {
        FragmentAgregarPresupuesto().apply {
            arguments = Bundle().apply {
                putStringArrayList("categorias_ocupadas", ArrayList(obtenerCategoriasOcupadas()))
            }
            setOnPresupuestoSaved { vm.agregarPresupuesto(it) }
        }.show(parentFragmentManager, "agregar_presupuesto")
    }

    private fun abrirEdicion(p: Presupuesto) {
        FragmentAgregarPresupuesto().apply {
            arguments = Bundle().apply {
                putStringArrayList("categorias_ocupadas", ArrayList(obtenerCategoriasOcupadas(p.id)))
            }
            editarPresupuesto(p) { vm.editarPresupuesto(it) }
        }.show(parentFragmentManager, "editar_presupuesto")
    }

    private fun obtenerCategoriasOcupadas(excluirId: String? = null): List<String> {
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        return vm.presupuestos.value?.filter { p ->
            p.id != excluirId && p.fechaFinal.after(hoy.time) || sameDay(p.fechaFinal, hoy.time)
        }?.map { it.categoriaNombre } ?: emptyList()
    }

    private fun actualizarUI(lista: List<Presupuesto>) {
        binding.containerPresupuestos.removeAllViews()

        if (lista.isEmpty()) {
            binding.containerPresupuestos.addView(TextView(requireContext()).apply {
                text = "No hay presupuestos. ¡Agrega uno!"
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, 200, 0, 200)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            })
            return
        }

        lista.forEach { p ->
            binding.containerPresupuestos.addView(crearItem(p))
        }
    }

    private fun crearItem(p: Presupuesto): View {
        val v = layoutInflater.inflate(R.layout.item_presupuesto, binding.containerPresupuestos, false)

        v.findViewById<TextView>(R.id.tvNombrePresupuesto).text = p.categoriaNombre
        v.findViewById<TextView>(R.id.tvCantidad).text = String.format("$%.2f", p.cantidad)
        v.findViewById<TextView>(R.id.tvFechaInicio).text = formatearFecha(p.fechaInicio)
        v.findViewById<TextView>(R.id.tvFechaFinal).text = formatearFecha(p.fechaFinal)
        v.findViewById<TextView>(R.id.tvCategoria).text = p.categoriaNombre

        val saldo = p.cantidad - p.montoGastado
        val tvSaldo = v.findViewById<TextView>(R.id.tvSaldoDisponible)
        tvSaldo.text = String.format("Saldo: $%.2f", saldo)
        tvSaldo.setTextColor(when {
            saldo < 0 -> ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            saldo < p.cantidad * 0.2 -> ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
            else -> ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
        })

        val tvEstado = v.findViewById<TextView>(R.id.tvEstadoPresupuesto)
        val expirado = p.fechaFinal.before(Date())
        tvEstado.text = if (expirado) "Expirado" else "Activo"
        tvEstado.setBackgroundResource(if (expirado) R.drawable.bg_estado_expirado else R.drawable.bg_estado_activo)

        v.findViewById<View>(R.id.btnEditar).setOnClickListener { abrirEdicion(p) }
        v.findViewById<View>(R.id.btnEliminar).setOnClickListener {
            vm.eliminarPresupuesto(p)
            Toast.makeText(requireContext(), "Presupuesto eliminado", Toast.LENGTH_SHORT).show()
        }

        return v
    }

    private fun sameDay(d1: Date, d2: Date): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(d1) == fmt.format(d2)
    }

    private fun formatearFecha(date: Date): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}