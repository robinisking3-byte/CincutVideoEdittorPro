package com.example.core.database

import android.content.Context
import androidx.room.*
import com.example.core.model.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllActiveProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("UPDATE projects SET isArchived = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun archiveProject(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET title = :newTitle, updatedAt = :timestamp WHERE id = :id")
    suspend fun renameProject(id: String, newTitle: String, timestamp: Long = System.currentTimeMillis())
}

@Database(entities = [Project::class], version = 1, exportSchema = false)
abstract class CineCutDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: CineCutDatabase? = null

        fun getInstance(context: Context): CineCutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CineCutDatabase::class.java,
                    "cinecut_studio.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
