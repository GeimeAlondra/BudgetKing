package com.example.gastosapp.Fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.gastosapp.Models.Categoria
import com.example.gastosapp.Models.Gasto
import com.example.gastosapp.R
import com.example.gastosapp.databinding.FragmentAgregarGastoBinding
import java.text.SimpleDateFormat
import java.util.*

class FragmentAgregarGasto : DialogFragment() {

    private var _binding: FragmentAgregarGastoBinding? = null
    private val binding get() = _binding!!

    private val calendar = Calendar.getInstance()
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var onGastoSaved: ((Gasto) -> Unit)? = null
    private var gastoAEditar: Gasto? = null

    fun setOnGastoSaved(listener: (Gasto) -> Unit) {
        onGastoSaved = listener
    }

    fun editarGasto(gasto: Gasto, listener: (Gasto) -> Unit) {
        gastoAEditar = gasto
        onGastoSaved = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAgregarGastoBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarCategoria()

        if (gastoAEditar != null) {
            precargarGasto(gastoAEditar!!)
        } else {
            limpiarCampos()
        }

        configurarFecha()

        binding.btnGuardarGasto.setOnClickListener { guardar() }
        binding.btnCancelarGasto.setOnClickListener { dismiss() }
    }

    private fun limpiarCampos() {
        binding.etNombreGasto.setText("")
        binding.etCantidadGasto.setText("")
        binding.etDescripcionGasto.setText("")
        // Resetear calendar a hoy y mostrar fecha actual
        calendar.time = Date()
        binding.etFechaGasto.text = sdf.format(calendar.time)
    }

    private fun configurarCategoria() {
        val categoriasValidas = arguments?.getStringArrayList("categorias_validas")
            ?: Categoria.values().map { it.nombre }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoriasValidas)
        binding.etCategoriaGasto.setAdapter(adapter)

        // Solo asignar la categoría por defecto si no estamos editando
        if (gastoAEditar == null) {
            val defecto = categoriasValidas.firstOrNull { it == "Otros" } ?: categoriasValidas.firstOrNull() ?: "Otros"
            binding.etCategoriaGasto.setText(defecto, false)
        }
    }

    private fun configurarFecha() {
        binding.btnFechaGasto.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, año, mes, dia ->
                    val seleccionada = Calendar.getInstance().apply { set(año, mes, dia) }

                    val hoy = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }
                    if (seleccionada.after(hoy)) {
                        Toast.makeText(
                            requireContext(),
                            "No puedes registrar gastos en fechas futuras",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Restablecer al día actual
                        calendar.time = Date()
                    } else {
                        calendar.set(año, mes, dia)
                    }
                    binding.etFechaGasto.text = sdf.format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun precargarGasto(gasto: Gasto) {
        binding.etNombreGasto.setText(gasto.nombre)
        binding.etCantidadGasto.setText(gasto.monto.toString())
        binding.etDescripcionGasto.setText(gasto.descripcion)
        binding.etCategoriaGasto.setText(gasto.categoriaNombre, false)
        calendar.time = gasto.fecha
        binding.etFechaGasto.text = sdf.format(gasto.fecha)
    }

    private fun guardar() {
        if (!validar()) return

        val nombreCat = binding.etCategoriaGasto.text.toString().trim()

        val gasto = gastoAEditar?.copy(
            nombre = binding.etNombreGasto.text.toString().trim(),
            monto = binding.etCantidadGasto.text.toString().toDouble(),
            descripcion = binding.etDescripcionGasto.text.toString().trim().ifEmpty { "" },
            categoriaNombre = nombreCat,
            fecha = calendar.time
        ) ?: Gasto(
            nombre = binding.etNombreGasto.text.toString().trim(),
            monto = binding.etCantidadGasto.text.toString().toDouble(),
            descripcion = binding.etDescripcionGasto.text.toString().trim().ifEmpty { "" },
            categoriaNombre = nombreCat,
            fecha = calendar.time
        )

        onGastoSaved?.invoke(gasto)
        dismiss()
        Toast.makeText(requireContext(), "Gasto guardado", Toast.LENGTH_SHORT).show()
    }

    private fun validar(): Boolean {
        var ok = true
        if (binding.etNombreGasto.text.isNullOrBlank()) {
            binding.etNombreGasto.error = "Requerido"
            ok = false
        }
        val montoStr = binding.etCantidadGasto.text.toString()
        if (montoStr.isBlank()) {
            binding.etCantidadGasto.error = "Requerido"
            ok = false
        } else if (montoStr.toDoubleOrNull() == null || montoStr.toDouble() <= 0) {
            binding.etCantidadGasto.error = "Ingresa un monto válido mayor a 0"
            ok = false
        }
        return ok
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}