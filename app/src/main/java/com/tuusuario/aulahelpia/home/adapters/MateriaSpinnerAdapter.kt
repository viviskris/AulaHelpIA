package com.tuusuario.aulahelpia.home.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.tuusuario.aulahelpia.R
import com.tuusuario.aulahelpia.home.utils.MateriasUtils

class MateriaSpinnerAdapter(
    context: Context,
    private val materias: List<String>
) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, materias) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_spinner_materia,
            parent,
            false
        )

        val materia = getItem(position) ?: ""
        val textView = view.findViewById<TextView>(R.id.tvMateriaSpinner)

        val emoji = MateriasUtils.getEmojiForMateria(materia)
        val color = MateriasUtils.getColorResForMateria(materia, context, position)

        textView.text = "$emoji $materia"
        textView.setTextColor(ContextCompat.getColor(context, color))

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_spinner_materia_dropdown,
            parent,
            false
        )

        val materia = getItem(position) ?: ""
        val textView = view.findViewById<TextView>(R.id.tvMateriaDropdown)

        val emoji = MateriasUtils.getEmojiForMateria(materia)
        val color = MateriasUtils.getColorResForMateria(materia, context, position)

        textView.text = "$emoji $materia"
        textView.setTextColor(ContextCompat.getColor(context, color))

        return view
    }
}