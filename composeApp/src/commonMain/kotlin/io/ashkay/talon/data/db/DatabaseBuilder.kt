package io.ashkay.talon.data.db

import androidx.room.RoomDatabase

expect fun getDatabaseBuilder(): RoomDatabase.Builder<TalonDatabase>
