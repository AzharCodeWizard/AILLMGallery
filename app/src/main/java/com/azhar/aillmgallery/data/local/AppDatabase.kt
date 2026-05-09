package com.azhar.aillmgallery.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.azhar.aillmgallery.data.local.dao.QuizDao
import com.azhar.aillmgallery.data.local.dao.StoryDao
import com.azhar.aillmgallery.data.local.entity.QuestionEntity
import com.azhar.aillmgallery.data.local.entity.QuizEntity
import com.azhar.aillmgallery.data.local.entity.StoryEntity

@Database(
    entities = [QuizEntity::class, QuestionEntity::class, StoryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun quizDao(): QuizDao
    abstract fun storyDao(): StoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_gallery_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
