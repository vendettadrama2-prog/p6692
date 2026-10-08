package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserByIdSync(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET demoBalance = demoBalance + :amount WHERE id = :id")
    suspend fun adjustBalance(id: String, amount: Double)

    @Query("UPDATE users SET pkrBalance = pkrBalance + :amount WHERE id = :id")
    suspend fun adjustPkrBalance(id: String, amount: Double)

    @Query("UPDATE users SET pkrBalance = :newBalance WHERE id = :id")
    suspend fun setPkrBalance(id: String, newBalance: Double)

    @Query("UPDATE users SET rewardPoints = rewardPoints + :points WHERE id = :id")
    suspend fun adjustPoints(id: String, points: Int)

    @Query("UPDATE users SET avatarUrl = :avatarUrl WHERE id = :id")
    suspend fun updateAvatar(id: String, avatarUrl: String)

    @Query("UPDATE users SET isBanned = :isBanned WHERE id = :id")
    suspend fun setBannedStatus(id: String, isBanned: Boolean)

    @Query("UPDATE users SET role = :role WHERE id = :id")
    suspend fun updateRole(id: String, role: String)

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>
}

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY playsCount DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE isEnabled = 1 ORDER BY playsCount DESC")
    fun getActiveGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE category = :category AND isEnabled = 1")
    fun getGamesByCategory(category: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun getGameById(id: String): GameEntity?

    @Query("SELECT COUNT(*) FROM games")
    suspend fun getGamesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("UPDATE games SET isEnabled = :enabled WHERE id = :id")
    suspend fun setGameEnabled(id: String, enabled: Boolean)

    @Query("UPDATE games SET isFeatured = :featured WHERE id = :id")
    suspend fun setGameFeatured(id: String, featured: Boolean)
}

@Dao
interface FavoriteDao {
    @Query("SELECT gameId FROM favorites WHERE userId = :userId")
    fun getFavoriteGameIds(userId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND gameId = :gameId")
    suspend fun removeFavorite(userId: String, gameId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND gameId = :gameId)")
    suspend fun isFavorite(userId: String, gameId: String): Boolean
}

@Dao
interface GameHistoryDao {
    @Query("SELECT * FROM game_history WHERE userId = :userId ORDER BY playedAt DESC LIMIT 50")
    fun getHistoryForUser(userId: String): Flow<List<GameHistoryEntity>>

    @Query("SELECT * FROM game_history ORDER BY playedAt DESC LIMIT 100")
    fun getAllHistory(): Flow<List<GameHistoryEntity>>

    @Insert
    suspend fun recordHistory(history: GameHistoryEntity)
}

// Server-Side Wallet Ledger DAO
@Dao
interface TransactionDao {
    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long
}

@Dao
interface WithdrawalDao {
    @Query("SELECT * FROM withdrawals WHERE userId = :userId ORDER BY requestedAt DESC")
    fun getWithdrawalsForUser(userId: String): Flow<List<WithdrawalEntity>>

    @Query("SELECT * FROM withdrawals ORDER BY requestedAt DESC")
    fun getAllWithdrawals(): Flow<List<WithdrawalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: WithdrawalEntity): Long

    @Query("UPDATE withdrawals SET status = :status, adminNotes = :notes, processedAt = :processedAt WHERE id = :id")
    suspend fun updateWithdrawalStatus(id: Long, status: String, notes: String, processedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM withdrawals WHERE id = :id LIMIT 1")
    suspend fun getWithdrawalById(id: Long): WithdrawalEntity?
}

@Dao
interface DailyRewardDao {
    @Query("SELECT * FROM daily_rewards ORDER BY day ASC")
    fun getDailyRewards(): Flow<List<DailyRewardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRewards(rewards: List<DailyRewardEntity>)

    @Query("UPDATE daily_rewards SET isClaimed = 1 WHERE day = :day")
    suspend fun claimReward(day: Int)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM admin_audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getLogs(): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insertLog(log: AuditLogEntity)
}

@Dao
interface DepositDao {
    @Query("SELECT * FROM deposits WHERE userId = :userId ORDER BY createdAt DESC")
    fun getDepositsForUser(userId: String): Flow<List<DepositEntity>>

    @Query("SELECT * FROM deposits ORDER BY createdAt DESC")
    fun getAllDeposits(): Flow<List<DepositEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeposit(deposit: DepositEntity): Long

    @Query("UPDATE deposits SET status = :status, adminNotes = :notes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDepositStatus(id: Long, status: String, notes: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM deposits WHERE id = :id LIMIT 1")
    suspend fun getDepositById(id: Long): DepositEntity?

    @Query("SELECT * FROM deposits WHERE orderId = :orderId LIMIT 1")
    suspend fun getDepositByOrderId(orderId: String): DepositEntity?
}

@Dao
interface JazzCashConfigDao {
    @Query("SELECT * FROM jazzcash_config WHERE id = 'primary_config' LIMIT 1")
    fun getConfig(): Flow<JazzCashConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: JazzCashConfigEntity)
}
