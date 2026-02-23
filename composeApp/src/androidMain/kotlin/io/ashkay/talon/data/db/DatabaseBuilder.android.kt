package io.ashkay.talon.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.mp.KoinPlatform

private const val DB_NAME = "talon.db"

actual fun getDatabaseBuilder(): RoomDatabase.Builder<TalonDatabase> {
  val context = KoinPlatform.getKoin().get<Context>()
  val dbFile = context.getDatabasePath(DB_NAME)
  return Room.databaseBuilder<TalonDatabase>(context, dbFile.absolutePath)
}
