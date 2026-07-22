package com.tuusuario.aulahelpia.home.adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuusuario.aulahelpia.R
import com.tuusuario.aulahelpia.home.data.PlanItem
import com.tuusuario.aulahelpia.home.data.TaskState
import java.time.LocalDate
import com.tuusuario.aulahelpia.home.dialogs.ReprogramCompleteDialog
import com.tuusuario.aulahelpia.home.utils.MateriasUtils

class TaskAdapter(
    private val onStateChanged: (PlanItem, TaskState) -> Unit,
    private val onDeleteClicked: (PlanItem) -> Unit
) : ListAdapter<PlanItem, TaskAdapter.TaskViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_evento_con_eliminar, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val event = getItem(position)
        holder.bind(event)
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvEventDescription)
        private val tvDate: TextView = itemView.findViewById(R.id.tvEventDate)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvEventCategory)
        private val tvCurrentState: TextView = itemView.findViewById(R.id.tvCurrentState)
        private val viewCategoryIndicator: View = itemView.findViewById(R.id.viewCategoryIndicator)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        // 🆕 BOTONES DE ESTADO
        private val btnStateCompletado: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnStateCompletado)
        private val btnStateVencido: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnStateVencido)
        private val btnStateReprogramado: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnStateReprogramado)

        fun bind(event: PlanItem) {
            // Información básica
            val position = adapterPosition + 1
            tvTitle.text = "$position. ${event.title}"
            tvDescription.text = event.description
            tvCategory.text = event.category ?: "Sin categoría"

            // Formatear fecha
            val eventDate = LocalDate.parse(event.date)
            val today = LocalDate.now()
            val dateDisplay = when {
                eventDate.isEqual(today) -> "📅 Hoy ${event.time}"
                eventDate.isBefore(today) -> "📅 ${eventDate.dayOfMonth}/${eventDate.monthValue} ${event.time}"
                else -> "📅 ${eventDate.dayOfMonth}/${eventDate.monthValue} ${event.time}"
            }
            tvDate.text = dateDisplay

            // Color de categoría
            // Obtener color según la materia
            val materia = event.category ?: "General"
            val colorRes = if (materia in MateriasUtils.materiasFijas.map { it.first }) {
                MateriasUtils.getColorRes(materia)
            } else {
                MateriasUtils.getColorForAdicionalPorNombre(materia)
            }
            val categoryColor = ContextCompat.getColor(itemView.context, colorRes)
            viewCategoryIndicator.setBackgroundColor(categoryColor)
            tvCategory.background.setTint(categoryColor)

            // 🆕 ESTADO ACTUAL
            updateStateDisplay(event)

            // 🆕 LISTENERS DE BOTONES DE ESTADO - PAUSA MEJORADA
            btnStateCompletado.setOnClickListener {
                onStateChanged(event, TaskState.COMPLETADO)
            }
            btnStateVencido.setOnClickListener {
                onStateChanged(event, TaskState.VENCIDO)
            }
            btnStateReprogramado.setOnClickListener {
                if (event.taskState == TaskState.ACTIVO.name) {
                    // 🆕 MOSTRAR DIÁLOGO DE REPROGRAMACIÓN
                    showReprogramDialog(event)
                } else {
                    // Si ya está REPROGRAMADO, cambiar a activo normal
                    onStateChanged(event, TaskState.ACTIVO)
                }
            }

            // Botón eliminar
            btnDelete.setOnClickListener {
                onDeleteClicked(event)
            }
        }

        // 🆕 ACTUALIZAR VISUALIZACIÓN DEL ESTADO
        private fun updateStateDisplay(event: PlanItem) {
            val state = TaskState.valueOf(event.taskState)

            tvCurrentState.text = when (state) {
                TaskState.ACTIVO -> "🔵 Activo"
                TaskState.COMPLETADO -> "✅ Completado"
                TaskState.VENCIDO -> "🔴 Vencido"
                TaskState.REPROGRAMADO -> "📅️ REPROGRAMADO"
            }

            // Colores según estado
            val stateColor = when (state) {
                TaskState.ACTIVO -> "#4361EE"
                TaskState.COMPLETADO -> "#4CAF50"
                TaskState.VENCIDO -> "#F44336"
                TaskState.REPROGRAMADO -> "#FF9800"
            }
            tvCurrentState.setBackgroundColor(android.graphics.Color.parseColor(stateColor))
        }

        // 🆕 AGREGAR ESTOS MÉTODOS NUEVOS
        private fun showReprogramDialog(event: PlanItem) {
            val activity = itemView.context as? androidx.fragment.app.FragmentActivity
            activity?.let {
                val dialog = ReprogramCompleteDialog(  // ← NUEVO DIÁLOGO
                    originalDate = event.date,
                    originalTime = event.time,
                    onReprogramComplete = { newDate, newTime, reason ->
                        // Llamar al método que maneja la reprogramación con motivo
                        updateEventWithNewDateTimeAndReason(event, newDate, newTime, reason)
                    }
                )
                dialog.show(it.supportFragmentManager, "ReprogramCompleteDialog")
            }
        }
        private fun updateEventWithNewDateTimeAndReason(event: PlanItem, newDate: String, newTime: String, reason: String?) {
            // Crear descripción con motivo (si existe)
            val reasonText = if (!reason.isNullOrEmpty()) {
                "📅 Reprogramado hasta $newDate a las $newTime\n💡 Motivo: $reason"
            } else {
                "📅 Reprogramado hasta $newDate a las $newTime"
            }

            // Crear copia del evento actualizado
            val updatedEvent = event.copy(
                date = newDate,
                time = newTime,
                taskState = TaskState.REPROGRAMADO.name,
                description = if (event.description.isNullOrEmpty()) {
                    reasonText
                } else {
                    "${event.description}\n$reasonText"
                }
            )

            // Llamar al ViewModel para actualizar
            onStateChanged(updatedEvent, TaskState.REPROGRAMADO)
        }

        private fun updateEventWithNewDateTime(event: PlanItem, newDate: String, newTime: String) {
            // Crear copia del evento con nueva fecha, hora y estado REPROGRAMADO
            val updatedEvent = event.copy(
                date = newDate,
                time = newTime,  // 🆕 NUEVA HORA TAMBIÉN
                taskState = TaskState.REPROGRAMADO.name,
                description = if (event.description.isNullOrEmpty()) {
                    "📅️ REPROGRAMADO hasta $newDate a las $newTime"
                } else {
                    "${event.description}\n📅️ REPROGRAMADO hasta $newDate a las $newTime"
                }
            )

            // Llamar al ViewModel para actualizar
            onStateChanged(updatedEvent, TaskState.REPROGRAMADO)
        }

    } // ← 🆕 AGREGAR ESTA LLAVE QUE FALTA (cierra TaskViewHolder)

} // ← ESTA CIERRA TaskAdapter

// 🆕 DIFCALLBACK FUERA DE LAS CLASES
private val DiffCallback = object : DiffUtil.ItemCallback<PlanItem>() {
    override fun areItemsTheSame(oldItem: PlanItem, newItem: PlanItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PlanItem, newItem: PlanItem): Boolean {
        return oldItem == newItem
    }
}