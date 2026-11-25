package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [ExamServerEntity::class, ExamSessionLogEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class ExamDatabase : RoomDatabase() {
  abstract fun examDao(): ExamDao

  companion object {
    @Volatile private var INSTANCE: ExamDatabase? = null

    fun getInstance(context: Context): ExamDatabase {
      return INSTANCE
        ?: synchronized(this) {
          val instance =
            Room.databaseBuilder(
                context.applicationContext,
                ExamDatabase::class.java,
                "examgate_database",
              )
              .build()
          INSTANCE = instance
          instance
        }
    }
  }
}
