package com.example.gastosapp.Fragments

import android.os.Bundle
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.gastosapp.Models.Categorias
import com.example.gastosapp.Models.Gasto
import com.example.gastosapp.R
import com.example.gastosapp.viewModels.GastoViewModel
import com.example.gastosapp.viewModels.PresupuestoViewModel
import java.util.logging.Handler

class FragmentGasto : Fragment() {

    private lateinit var gastoViewModel: GastoViewModel
    private lateinit var contenedorGastos: LinearLayout
    private lateinit var botonAgregar: LottieAnimationView
    private lateinit var presupuestoViewModel: PresupuestoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gastoViewModel = ViewModelProvider(requireActivity())[GastoViewModel::class.java]
        presupuestoViewModel = ViewModelProvider(requireActivity())[PresupuestoViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val vista = inflater.inflate(R.layout.fragment_gasto, container, false)
        contenedorGastos = vista.findViewById(R.id.containerGastos)
        botonAgregar = vista.findViewById(R.id.agregarGasto)

        // Observar cambios en la lista
        gastoViewModel.gastos.observe(viewLifecycleOwner) { lista ->
            actualizarListaGastos(lista)
        }

        botonAgregar.setOnClickListener {
            botonAgregar.playAnimation()
            mostrarDialogoAgregarGasto()
        }

        return vista
    }

    private fun abrirDialogoAgregarGasto() {
        val dialog = FragmentAgregarGasto()

        // Obtener las categorías con presupuesto activo desde el PresupuestoViewModel
        val categoriasConPresupuesto = presupuestoViewModel.presupuestos.value
            ?.filter { it.cantidad > it.montoGastado } // Categorías con saldo restante
            ?.map { Categorias.getNombrePorId(it.categoriaId) }
            ?.distinct()
            ?: listOf()

        dialog.arguments = Bundle().apply {
            putStringArrayList("categorias_validas", ArrayList(categoriasConPresupuesto))
        }

        dialog.setGastoGuardadoListener(object : FragmentAgregarGasto.GastoGuardadoListener {
            override fun onGastoGuardado(gasto: Gasto) {
                // Llamamos a la función del GastoViewModel
                gastoViewModel.agregarGasto(gasto) { exito, mensaje ->
                    Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                }
            }
        })
        dialog.show(parentFragmentManager, "agregarGasto")
    }

    private fun mostrarDialogoAgregarGasto() {
        val dialog = FragmentAgregarGasto()

        // Obtener las categorías con presupuesto activo
        val categoriasConPresupuesto = presupuestoViewModel.presupuestos.value
            ?.filter { it.cantidad > it.montoGastado } // Categorías con saldo
            ?.map { Categorias.getNombrePorId(it.categoriaId) }
            ?.distinct()
            ?: listOf()

        dialog.arguments = Bundle().apply {
            putStringArrayList("categorias_validas", ArrayList(categoriasConPresupuesto))
        }

        dialog.setGastoGuardadoListener(object : FragmentAgregarGasto.GastoGuardadoListener {
            override fun onGastoGuardado(gasto: Gasto) {
                gastoViewModel.agregarGasto(gasto) { exito, mensaje ->
                    val msg = if (exito) "Gasto agregado" else mensaje ?: "Error"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            }
        })
        dialog.show(parentFragmentManager, "agregar_gasto")
    }

    private fun actualizarListaGastos(lista: List<Gasto>) {
        contenedorGastos.removeAllViews()

        if (lista.isEmpty()) {
            contenedorGastos.addView(TextView(context).apply {
                text = "No hay gastos registrados"
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, 120, 0, 120)
                setTextColor(requireContext().getColor(android.R.color.darker_gray))
            })
            return
        }

        lista.forEachIndexed { index, gasto ->
            contenedorGastos.addView(crearFilaGasto(gasto, index))
        }
    }

    private fun crearFilaGasto(gasto: Gasto, posicion: Int): View {
        val fila = LayoutInflater.from(context).inflate(R.layout.item_gasto, contenedorGastos, false)

        // Asignar datos
        fila.findViewById<TextView>(R.id.tvNombreGasto).text = gasto.nombre ?: "Sin nombre"
        fila.findViewById<TextView>(R.id.tvCantidadGasto).text = "-$${String.format("%.2f", gasto.monto)}"
        fila.findViewById<TextView>(R.id.tvDescripcion).text = gasto.descripcion ?: "Sin descripción"
        fila.findViewById<TextView>(R.id.tvCategoria).text = Categorias.getNombrePorId(gasto.categoriaId)
        fila.findViewById<TextView>(R.id.tvFechaGasto).text = gasto.fecha ?: "--/--/----"

        // Botón Editar
        fila.findViewById<View>(R.id.btnEditarGasto).setOnClickListener {
            dialogEditarGasto(gasto)
        }

        // Botón Eliminar
        fila.findViewById<View>(R.id.btnEliminarGasto).setOnClickListener {
            dialogEliminar(gasto, posicion)
        }

        return fila
    }

    private fun dialogEditarGasto(gasto: Gasto) {
        try {
            val dialog = FragmentAgregarGasto()

            val categoriasConPresupuesto = presupuestoViewModel.presupuestos.value
                ?.filter { it.cantidad > it.montoGastado }
                ?.map { Categorias.getNombrePorId(it.categoriaId) }
                ?.distinct()
                ?: listOf()

            dialog.arguments = Bundle().apply {
                putStringArrayList("categorias_validas", ArrayList(categoriasConPresupuesto))
            }

            dialog.setGastoAEditar(gasto, object : FragmentAgregarGasto.GastoEditadoListener {
                override fun onGastoEditado(gastoEditado: Gasto) {
                    try {
                        android.os.Handler(Looper.getMainLooper()).postDelayed({
                            if (gastoEditado.id.isNullOrEmpty()) {
                                Toast.makeText(requireContext(), "Error: ID nulo", Toast.LENGTH_SHORT).show()
                                return@postDelayed
                            }

                            val pos = gastoViewModel.obtenerPosicionPorId(gastoEditado.id)
                            if (pos != -1) {
                                gastoViewModel.editarGasto(gastoEditado, pos) { exito, mensaje ->
                                    val msg = if (exito) "Gasto actualizado" else mensaje ?: "Error"
                                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(requireContext(), "Error: Gasto no encontrado", Toast.LENGTH_SHORT).show()
                            }
                        }, 100) // ← Pequeño delay
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })

            dialog.show(parentFragmentManager.beginTransaction(), "editar_gasto_${System.currentTimeMillis()}")

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al abrir editor", Toast.LENGTH_SHORT).show()
        }

    }


    private fun dialogEliminar(gasto: Gasto, posicion: Int) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar gasto")
            .setMessage("¿Eliminar '${gasto.nombre}' de $${gasto.monto}?")
            .setPositiveButton("Eliminar") { _, _ ->
                gastoViewModel.eliminarGasto(posicion) { exito, mensaje ->
                    val msg = if (exito) "Gasto eliminado" else mensaje ?: "Error"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}