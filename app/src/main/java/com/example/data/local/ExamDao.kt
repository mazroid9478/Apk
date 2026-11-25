package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
  @Query("SELECT * FROM exam_servers ORDER BY createdAt DESC")
  fun observeAllServers(): Flow<List<ExamServerEntity>>

  @Query("SELECT * FROM exam_servers WHERE id = :id LIMIT 1")
  suspend fun getServerById(id: Long): ExamServerEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertServer(server: ExamServerEntity): Long

  @Update
  suspend fun updateServer(server: ExamServerEntity)

  @Query("DELETE FROM exam_servers WHERE id = :id")
  suspend fun deleteServerById(id: Long)

  @Query("SELECT * FROM exam_session_logs ORDER BY startedAt DESC")
  fun observeSessionLogs(): Flow<List<ExamSessionLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSessionLog(log: ExamSessionLogEntity): Long

  @Update
  suspend fun updateSessionLog(log: ExamSessionLogEntity)

  @Query("DELETE FROM exam_session_logs")
  suspend fun clearAllSessionLogs()
}
