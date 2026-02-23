package io.ashkay.talon.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSHomeDirectory

private const val DB_NAME = "talon.db"

actual fun getDatabaseBuilder(): RoomDatabase.Builder<TalonDatabase> {
  val dbFilePath = NSHomeDirectory() + "/$DB_NAME"
  return Room.databaseBuilder<TalonDatabase>(name = dbFilePath)
}
