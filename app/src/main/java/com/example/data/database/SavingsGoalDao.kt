package com.example.data.database

import androidx.room.*
import com.example.domain.model.SavingsGoal
import com.example.domain.model.PrivateVaultState
import com.example.domain.model.VaultHistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY priority DESC, createdDate DESC")
    fun getAllGoals(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getGoalById(id: Int): SavingsGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal): Long

    @Update
    suspend fun updateGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoal)
}

@Dao
interface PrivateVaultDao {
    @Query("SELECT * FROM private_vault_state WHERE id = 1")
    fun getVaultStateFlow(): Flow<PrivateVaultState?>

    @Query("SELECT * FROM private_vault_state WHERE id = 1")
    suspend fun getVaultStateOneShot(): PrivateVaultState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateState(state: PrivateVaultState)

    @Query("SELECT * FROM vault_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<VaultHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: VaultHistoryItem): Long
}
