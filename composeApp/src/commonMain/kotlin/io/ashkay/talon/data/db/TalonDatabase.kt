package io.ashkay.talon.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [AgentSessionEntity::class, LogEntryEntity::class], version = 1)
@ConstructedBy(TalonDatabaseConstructor::class)
abstract class TalonDatabase : RoomDatabase() {
  abstract fun sessionDao(): SessionDao

  abstract fun logEntryDao(): LogEntryDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object TalonDatabaseConstructor : RoomDatabaseConstructor<TalonDatabase> {
  override fun initialize(): TalonDatabase
}
