package ru.vlyashuk.roadbuddy.data.local.database

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase

object AndroidContextProvider {
    lateinit var context: Application
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val appContext = AndroidContextProvider.context.applicationContext
    val dbFile = appContext.getDatabasePath("roadbuddy.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
        factory = { AppDatabaseConstructor.initialize() }
    )
}