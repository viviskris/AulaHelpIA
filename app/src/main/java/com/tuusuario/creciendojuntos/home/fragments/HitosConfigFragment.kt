package com.tuusuario.creciendojuntos.home.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tuusuario.creciendojuntos.R
import com.tuusuario.creciendojuntos.home.data.EventType
import com.tuusuario.creciendojuntos.home.viewmodel.PregnancyViewModel
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class HitosConfigFragment : Fragment() {

    private lateinit var recyclerEcografias: RecyclerView
    private lateinit var recyclerCitas: RecyclerView
    private lateinit var recyclerHitos: RecyclerView

    private val viewModel: PregnancyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_hitos_config, container, false)
        setupRecyclerViews(view)
        setupClickListeners(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar AdMob
        MobileAds.initialize(requireContext())
        val adRequest = AdRequest.Builder().build()

        // Como usas inflate en lugar de view binding, usa findViewById:
        val adView = view.findViewById<AdView>(R.id.adViewHitos)
        adView.loadAd(adRequest)
        actualizarListas()
    }

    // 🆕 MÉTODO PARA ELIMINAR EVENTO (EN EL FRAGMENT, NO EN EL ADAPTADOR)
    private fun eliminarEvento(tipoEvento: EventType, position: Int) {
        try {
            val eventos = viewModel.getEventsByType(tipoEvento, requireContext())
            if (position < eventos.size) {
                val eventoAEliminar = eventos[position]

                AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar Evento")
                    .setMessage("¿Estás segura de que quieres eliminar '${eventoAEliminar.title}'?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        viewModel.deleteEvent(eventoAEliminar.id, requireContext())
                        Toast.makeText(requireContext(), "Evento eliminado", Toast.LENGTH_SHORT).show()
                        actualizarListas()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al eliminar evento", Toast.LENGTH_SHORT).show()
            println("❌ Error eliminando evento: ${e.message}")
        }
    }

    private fun setupRecyclerViews(view: View) {
        recyclerEcografias = view.findViewById(R.id.recycler_ecografias)
        recyclerCitas = view.findViewById(R.id.recycler_citas)
        recyclerHitos = view.findViewById(R.id.recycler_hitos)

        // 🆕 ADAPTADORES CON FUNCIÓN DE ELIMINAR
        val adaptadorEcografias = HitosAdapter(mutableListOf(), EventType.ULTRASOUND) { position ->
            eliminarEvento(EventType.ULTRASOUND, position)
        }
        val adaptadorCitas = HitosAdapter(mutableListOf(), EventType.MEDICAL_APPOINTMENT) { position ->
            eliminarEvento(EventType.MEDICAL_APPOINTMENT, position)
        }
        val adaptadorHitos = HitosAdapter(mutableListOf(), EventType.PERSONAL_MILESTONE) { position ->
            eliminarEvento(EventType.PERSONAL_MILESTONE, position)
        }

        recyclerEcografias.adapter = adaptadorEcografias
        recyclerCitas.adapter = adaptadorCitas
        recyclerHitos.adapter = adaptadorHitos

        recyclerEcografias.layoutManager = LinearLayoutManager(requireContext())
        recyclerCitas.layoutManager = LinearLayoutManager(requireContext())
        recyclerHitos.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<Button>(R.id.btn_agregar_ecografia).setOnClickListener {
            mostrarDialogoSeleccionarFecha(EventType.ULTRASOUND)
        }

        view.findViewById<Button>(R.id.btn_agregar_cita).setOnClickListener {
            mostrarDialogoSeleccionarFecha(EventType.MEDICAL_APPOINTMENT)
        }

        view.findViewById<Button>(R.id.btn_agregar_hito).setOnClickListener {
            mostrarDialogoSeleccionarFecha(EventType.PERSONAL_MILESTONE)
        }
    }

    private fun mostrarDialogoSeleccionarFecha(tipoEvento: EventType) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_seleccionar_fecha_con_hora, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val datePicker = dialogView.findViewById<DatePicker>(R.id.date_picker)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.et_descripcion)

        val titulo = when(tipoEvento) {
            EventType.ULTRASOUND -> "Nueva Ecografía"
            EventType.MEDICAL_APPOINTMENT -> "Nueva Cita Médica"
            EventType.PERSONAL_MILESTONE -> "Nuevo Hito Personal"
            else -> "Evento Personal"
        }
        dialogView.findViewById<TextView>(R.id.tv_titulo_dialog).text = titulo

        timePicker.setIs24HourView(true)
        timePicker.hour = 9
        timePicker.minute = 0

        dialogView.findViewById<Button>(R.id.btn_cancelar).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_guardar).setOnClickListener {
            val descripcion = etDescripcion.text.toString()
            if (descripcion.isNotEmpty()) {
                val selectedDate = LocalDate.of(
                    datePicker.year,
                    datePicker.month + 1,
                    datePicker.dayOfMonth
                )

                val hora = timePicker.hour.toString().padStart(2, '0')
                val minuto = timePicker.minute.toString().padStart(2, '0')
                val timeString = "$hora:$minuto"

                when(tipoEvento) {
                    EventType.ULTRASOUND -> viewModel.addUltrasoundEvent(selectedDate, timeString, descripcion, requireContext())
                    EventType.MEDICAL_APPOINTMENT -> viewModel.addMedicalAppointment(selectedDate, timeString, descripcion, requireContext())
                    EventType.PERSONAL_MILESTONE -> viewModel.addPersonalMilestone(selectedDate, timeString, descripcion, requireContext())
                    else -> {
                        val event = PregnancyViewModel.CalendarEvent(
                            title = "Evento: $descripcion",
                            description = descripcion,
                            date = selectedDate,
                            time = LocalTime.parse(timeString),
                            type = tipoEvento.name
                        )
                        viewModel.addEvent(event, requireContext())
                    }
                }

                Toast.makeText(requireContext(), "$titulo guardado para ${selectedDate.dayOfMonth}/${selectedDate.monthValue} a las $timeString", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                actualizarListas()
            } else {
                Toast.makeText(requireContext(), "Ingresa una descripción", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun actualizarListas() {
        try {
            val ecografias = viewModel.getEventsByType(EventType.ULTRASOUND, requireContext())
            val citas = viewModel.getEventsByType(EventType.MEDICAL_APPOINTMENT, requireContext())
            val hitos = viewModel.getEventsByType(EventType.PERSONAL_MILESTONE, requireContext())

            println("🔄 ACTUALIZANDO LISTAS:")
            println("📊 Ecografías: ${ecografias.size} eventos")
            println("📊 Citas médicas: ${citas.size} eventos")
            println("📊 Hitos personales: ${hitos.size} eventos")

            (recyclerEcografias.adapter as? HitosAdapter)?.actualizarDatos(ecografias.map { it.description })
            (recyclerCitas.adapter as? HitosAdapter)?.actualizarDatos(citas.map { it.description })
            (recyclerHitos.adapter as? HitosAdapter)?.actualizarDatos(hitos.map { it.description })

        } catch (e: Exception) {
            println("⚠️ Error actualizando listas: ${e.message}")
            e.printStackTrace()
        }
    }
}

// 🆕 ADAPTADOR MEJORADO CON ELIMINACIÓN
class HitosAdapter(
    private var items: List<String>,
    private val tipoEvento: EventType,
    private val onDeleteClick: (Int) -> Unit // 🆕 CALLBACK PARA ELIMINAR
) : RecyclerView.Adapter<HitosAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
        val deleteButton: ImageButton = view.findViewById(R.id.btn_eliminar) // 🆕 BOTÓN ELIMINAR
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_evento_con_eliminar, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (items.isEmpty()) {
            holder.textView.text = "No hay eventos configurados"
            holder.deleteButton.visibility = View.GONE
        } else {
            holder.textView.text = items[position]
            holder.deleteButton.visibility = View.VISIBLE
            holder.deleteButton.setOnClickListener {
                onDeleteClick(position)
            }
        }
    }

    fun actualizarDatos(nuevosItems: List<String>) {
        this.items = nuevosItems
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return if (items.isEmpty()) 1 else items.size
    }
}