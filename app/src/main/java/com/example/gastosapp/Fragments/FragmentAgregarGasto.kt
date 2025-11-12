package com.example.gastosapp.Fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.gastosapp.Models.Categorias
import com.example.gastosapp.Models.Gasto
import com.example.gastosapp.R
import com.google.android.material.textfield.TextInputEditText
import java.util.*
import java.util.logging.Handler

class FragmentAgregarGasto : DialogFragment() {

    private lateinit var etNombreGasto: TextInputEditText
    private lateinit var etCantidadGasto: TextInputEditText
    private lateinit var etDescripcionGasto: TextInputEditText
    private lateinit var etCategoriaGasto: AutoCompleteTextView
    private lateinit var etFechaGasto: TextView
    private lateinit var btnFechaGasto: Button
    private lateinit var rootView: View

    private val calendar = Calendar.getInstance()
    private var fechaSeleccionada: String = ""

    private var listenerCrear: GastoGuardadoListener? = null
    private var listenerEditar: GastoEditadoListener? = null
    private var gastoAEditar: Gasto? = null

    fun setGastoGuardadoListener(listener: GastoGuardadoListener) {
        this.listenerCrear = listener
    }

    fun setGastoAEditar(gasto: Gasto, listener: GastoEditadoListener) {
        this.gastoAEditar = gasto
        this.listenerEditar = listener
    }

    interface GastoGuardadoListener { fun onGastoGuardado(g: Gasto) }
    interface GastoEditadoListener { fun onGastoEditado(g: Gasto) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_agregar_gasto, container, false)

        // ELIMINAR EL FONDO DEL DIÁLOGO PARA VER LAS ESQUINAS REDONDAS
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setDimAmount(0.3f) // Opcional: oscurecimiento del fondo

        initViews()
        configurarCategoria()
        configurarSelectorFecha()
        configurarBotones()

        gastoAEditar?.let { precargarDatos(it) }
        return rootView
    }

    private fun initViews() {
        etNombreGasto = rootView.findViewById(R.id.etNombreGasto)
        etCantidadGasto = rootView.findViewById(R.id.etCantidadGasto)
        etDescripcionGasto = rootView.findViewById(R.id.etDescripcionGasto)
        etCategoriaGasto = rootView.findViewById(R.id.etCategoriaGasto)
        etFechaGasto = rootView.findViewById(R.id.etFechaGasto)
        btnFechaGasto = rootView.findViewById(R.id.btnFechaGasto)

        // Fecha por defecto: hoy
        actualizarFechaHoy()
    }

    private fun actualizarFechaHoy() {
        val hoy = Calendar.getInstance()
        calendar.time = hoy.time
        fechaSeleccionada = formatearFecha(hoy)
        etFechaGasto.text = fechaSeleccionada
        btnFechaGasto.text = "Cambiar"
    }

    private fun configurarCategoria() {
        // Obtener categorías válidas desde arguments o usar todas
        val categoriasParaMostrar = arguments?.getStringArrayList("categorias_validas")
            ?: Categorias.lista.map { it.nombre }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoriasParaMostrar)
        etCategoriaGasto.setAdapter(adapter)

        // Establecer valor por defecto solo si hay categorías disponibles
        if (categoriasParaMostrar.isNotEmpty()) {
            val categoriaDefecto = if ("Otros" in categoriasParaMostrar) "Otros" else categoriasParaMostrar[0]
            etCategoriaGasto.setText(categoriaDefecto, false)
        } else {
            etCategoriaGasto.setText("No hay categorías disponibles", false)
            etCategoriaGasto.isEnabled = false
        }
    }

    private fun configurarSelectorFecha() {
        btnFechaGasto.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, año, mes, dia ->
                    calendar.set(año, mes, dia)
                    fechaSeleccionada = formatearFecha(calendar)
                    etFechaGasto.text = fechaSeleccionada
                    btnFechaGasto.text = "Seleccionar"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun configurarBotones() {
        rootView.findViewById<Button>(R.id.btnGuardarGasto).setOnClickListener { guardarGasto() }
        rootView.findViewById<Button>(R.id.btnCancelarGasto).setOnClickListener { dismiss() }
    }

    private fun precargarDatos(gasto: Gasto) {
        etNombreGasto.setText(gasto.nombre)
        etCantidadGasto.setText(gasto.monto.toString())
        etDescripcionGasto.setText(gasto.descripcion)
        etCategoriaGasto.setText(Categorias.getNombrePorId(gasto.categoriaId), false)

        gasto.fecha?.let { fechaStr ->
            parsearYMostrarFecha(fechaStr)
        }
    }

    private fun parsearYMostrarFecha(fecha: String) {
        val partes = fecha.split("/")
        if (partes.size == 3) {
            calendar.set(partes[2].toInt(), partes[1].toInt() - 1, partes[0].toInt())
            fechaSeleccionada = fecha
            etFechaGasto.text = fecha
            btnFechaGasto.text = "Cambiar"
        }
    }

    private fun guardarGasto() {
        if (!validarCampos()) return

        try {
            val nombre = etNombreGasto.text.toString().trim()
            val monto = etCantidadGasto.text.toString().toDouble()
            val descripcion = etDescripcionGasto.text.toString().trim().ifEmpty { null }
            val categoriaNombre = etCategoriaGasto.text.toString()
            val categoriaId = Categorias.getIdPorNombre(categoriaNombre)

            val categoriasValidas = arguments?.getStringArrayList("categorias_validas")
            if (categoriasValidas != null && categoriaNombre !in categoriasValidas) {
                Toast.makeText(requireContext(), "No hay presupuesto disponible para esta categoría", Toast.LENGTH_LONG).show()
                return
            }

            val gasto = if (gastoAEditar != null) {
                gastoAEditar!!.copy(
                    nombre = nombre,
                    monto = monto,
                    descripcion = descripcion,
                    categoriaId = categoriaId,
                    fecha = fechaSeleccionada
                )
            } else {
                Gasto(
                    nombre = nombre,
                    descripcion = descripcion,
                    monto = monto,
                    categoriaId = categoriaId,
                    fecha = fechaSeleccionada
                )
            }

            val context = requireContext()

            if (gastoAEditar != null) {
                listenerEditar?.onGastoEditado(gasto)
            } else {
                listenerCrear?.onGastoGuardado(gasto)
            }

            dismiss()

            android.os.Handler(Looper.getMainLooper()).postDelayed({
                Toast.makeText(context, "Gasto guardado", Toast.LENGTH_SHORT).show()
            }, 200)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }    private fun validarCampos(): Boolean {
        if (etNombreGasto.text.isNullOrBlank()) {
            etNombreGasto.error = "Nombre requerido"
            return false
        }
        if (etCantidadGasto.text.isNullOrBlank()) {
            etCantidadGasto.error = "Monto requerido"
            return false
        }
        val cat = etCategoriaGasto.text.toString()
        if (cat !in Categorias.lista.map { it.nombre }) {
            etCategoriaGasto.error = "Categoría inválida"
            return false
        }
        if (etFechaGasto.text == "No seleccionada") {
            Toast.makeText(requireContext(), "Selecciona una fecha", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun formatearFecha(cal: Calendar): String {
        return String.format(
            "%02d/%02d/%d",
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR)
        )
    }
}