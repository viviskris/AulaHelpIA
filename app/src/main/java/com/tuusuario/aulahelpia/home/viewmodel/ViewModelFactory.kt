package com.tuusuario.aulahelpia.home.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuusuario.aulahelpia.home.data.AppDatabase
import android.app.Application

class ViewModelFactory(private val context: Context, private val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TaskViewModel::class.java) -> {
                val eventDao = AppDatabase.getDatabase(context).eventDao()
                TaskViewModel(eventDao) as T
            }
            modelClass.isAssignableFrom(CalendarViewModel::class.java) -> {
                val eventDao = AppDatabase.getDatabase(context).eventDao()
                CalendarViewModel(eventDao, application) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                val eventDao = AppDatabase.getDatabase(context).eventDao()
                ProfileViewModel(eventDao, context) as T
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}