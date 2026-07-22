package com.tuusuario.aulahelpia.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tuusuario.aulahelpia.R
import com.tuusuario.aulahelpia.home.data.HorarioItem
import com.tuusuario.aulahelpia.home.utils.MateriasUtils

class HorarioSemanalAdapter(
    private var horarioPorDia: Map<String, List<HorarioItem>>,
    private val onItemLongClick: ((HorarioItem) -> Unit)? = null
) : RecyclerView.Adapter<HorarioSemanalAdapter.HorarioViewHolder>() {

    private val dias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")
    private val totalColumnas = 6 // Hora + 5 días

    // Extraer todas las horas únicas de las clases
    private fun getHorasUnicas(): List<String> {
        val horas = mutableSetOf<String>()
        horarioPorDia.values.forEach { clases ->
            clases.forEach { horas.add(it.horaInicio) }
        }
        return horas.sortedWith(compareBy {
            val partes = it.split(":")
            if (partes.size == 2) {
                (partes[0].toIntOrNull() ?: 0) * 60 + (partes[1].toIntOrNull() ?: 0)
            } else {
                0
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_horario_celda, parent, false)
        return HorarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: HorarioViewHolder, position: Int) {
        val horas = getHorasUnicas()
        val totalFilas = horas.size + 1 // +1 por la cabecera

        val fila = position / totalColumnas
        val columna = position % totalColumnas

        if (fila == 0) {
            // Primera fila: Cabecera
            when (columna) {
                0 -> {
                    holder.tvCelda.text = "Hora"
                    holder.tvCelda.setTextColor(holder.itemView.context.getColor(R.color.purple_neon))
                    holder.tvCelda.setBackgroundColor(holder.itemView.context.getColor(R.color.surface_dark))
                    holder.tvCelda.setPadding(0, 0, 0, 0)
                }
                else -> {
                    val diaIndex = columna - 1
                    if (diaIndex < dias.size) {
                        holder.tvCelda.text = dias[diaIndex]
                        holder.tvCelda.setTextColor(holder.itemView.context.getColor(R.color.purple_neon))
                        holder.tvCelda.setBackgroundColor(holder.itemView.context.getColor(R.color.surface_dark))
                        holder.tvCelda.setPadding(0, 0, 0, 0)
                    }
                }
            }
            return
        }

        // Resto de filas: Datos
        val horaIndex = fila - 1
        if (horaIndex < horas.size) {
            val horaActual = horas[horaIndex]

            when (columna) {
                0 -> {
                    // Primera columna: Hora
                    // Buscar la clase que comienza en esta hora para mostrar el rango
                    var bloqueHorario = horaActual
                    for (clases in horarioPorDia.values) {
                        val clase = clases.find { it.horaInicio == horaActual }
                        if (clase != null) {
                            bloqueHorario = "${clase.horaInicio} - ${clase.horaFin}"
                            break
                        }
                    }
                    holder.tvCelda.text = bloqueHorario
                    holder.tvCelda.minWidth = 60
                    holder.tvCelda.setTextColor(holder.itemView.context.getColor(R.color.text_secondary))
                    holder.tvCelda.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    holder.tvCelda.setPadding(0, 0, 0, 0)
                }
                else -> {
                    // Columnas de días
                    val diaIndex = columna - 1
                    if (diaIndex < dias.size) {
                        val dia = dias[diaIndex]
                        val clases = horarioPorDia[dia] ?: emptyList()

                        // Buscar clase en esta hora
                        val clase = clases.find { it.horaInicio == horaActual }

                        if (clase != null) {
                            val emoji = MateriasUtils.getEmojiForMateria(clase.materia)
                            var texto = "$emoji ${clase.materia}"

                            if (clase.profesor.isNotEmpty()) {
                                texto += "\n👨‍🏫 ${clase.profesor}"
                            }

                            if (clase.aula.isNotEmpty()) {
                                texto += "\n🏫 ${clase.aula}"
                            }

                            holder.tvCelda.text = texto
                            holder.tvCelda.minWidth = 120
                            holder.tvCelda.maxWidth = 180
                            holder.tvCelda.setTextColor(holder.itemView.context.getColor(R.color.white))
                            val color = MateriasUtils.getColorForAdicionalPorNombre(clase.materia)
                            holder.tvCelda.setBackgroundColor(holder.itemView.context.getColor(color))
                            holder.tvCelda.setPadding(6, 6, 6, 6)

                            // 🆕 CLIC CORTO → CREAR TAREA
                            holder.itemView.setOnClickListener {
                                val navController = androidx.navigation.Navigation.findNavController(
                                    holder.itemView
                                )
                                val bundle = android.os.Bundle().apply {
                                    putString("selectedMateria", clase.materia)
                                }
                                navController.navigate(
                                    com.tuusuario.aulahelpia.R.id.navigation_new_task,
                                    bundle
                                )
                                println("📝 Crear tarea desde horario: ${clase.materia}")
                            }

                            // CLIC LARGO → ELIMINAR (ya existente)
                            holder.itemView.setOnLongClickListener {
                                onItemLongClick?.invoke(clase)
                                true
                            }
                        }else {
                            holder.tvCelda.text = "-"
                            holder.tvCelda.minWidth = 100
                            holder.tvCelda.maxWidth = 140
                            holder.tvCelda.setTextColor(holder.itemView.context.getColor(R.color.text_secondary))
                            holder.tvCelda.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            holder.tvCelda.setPadding(0, 0, 0, 0)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        val horas = getHorasUnicas()
        return (horas.size + 1) * totalColumnas // +1 por la cabecera
    }

    fun actualizarHorario(nuevoHorario: Map<String, List<HorarioItem>>) {
        this.horarioPorDia = nuevoHorario
        notifyDataSetChanged()
    }

    class HorarioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCelda: TextView = view.findViewById(R.id.tvCeldaHorario)
    }
}