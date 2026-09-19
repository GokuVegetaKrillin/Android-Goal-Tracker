package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.dao.GoalDao
import com.example.data.dao.GoalNoteDao
import com.example.data.dao.TimeLogDao
import com.example.data.model.GoalEntity
import com.example.data.model.GoalNoteEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.TimeLogEntity

class Converters {
    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType): String {
        return value.name
    }

    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType {
        return try {
            RecurrenceType.valueOf(value)
        } catch (e: Exception) {
            RecurrenceType.DAILY
        }
    }
}

@Database(
    entities = [GoalEntity::class, TimeLogEntity::class, GoalNoteEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun timeLogDao(): TimeLogDao
    abstract fun goalNoteDao(): GoalNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "goal_tracker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
