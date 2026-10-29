package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "p6692_user_1",
    val username: String = "CyberPlayer_77",
    val mobileNumber: String = "03001234567",
    val email: String = "player@p6692.com",
    val avatarUrl: String = "",
    val vipLevel: Int = 1, // VIP 1 - 7
    val pkrBalance: Double = 0.0, // Real PKR Wallet Balance (Starts strictly at PKR 0.00 in production)
    val demoBalance: Double = 5000.0, // Initial DEMO balance: PKR 5,000 (DEMO BALANCE — NOT REAL MONEY)
    val rewardPoints: Int = 500,
    val isBanned: Boolean = false,
    val role: String = "PLAYER", // PLAYER, ADMIN
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String, // "Crash", "Slots", "Mines", "Cards", "Arcade", "Mini Games"
    val description: String,
    val rating: Float = 4.9f,
    val playsCount: Int = 124500,
    val isFeatured: Boolean = false,
    val isPopular: Boolean = false,
    val isNew: Boolean = false,
    val minBetPkr: Double = 50.0,
    val maxBetPkr: Double = 50000.0,
    val maxMultiplier: String = "5,000x",
    val isEnabled: Boolean = true,
    val rtpPercent: Float = 97.5f,
    val accentColorHex: String = "#8B5CF6"
)

@Entity(tableName = "favorites", primaryKeys = ["userId", "gameId"])
data class FavoriteEntity(
    val userId: String,
    val gameId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "game_history")
data class GameHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val gameId: String,
    val gameTitle: String,
    val betAmountPkr: Double,
    val payoutPkr: Double,
    val multiplier: Double,
    val resultType: String, // WIN, LOSS, JACKPOT
    val playedAt: Long = System.currentTimeMillis()
)

// Server-Side Wallet Ledger
@Entity(tableName = "wallet_transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "p6692_user_1",
    val transactionId: String = "", // e.g. TXN-BET-92841, TXN-WIN-92842, TXN-DEP-84920
    val type: String, // DEPOSIT, BET, WIN, LOSS, WITHDRAWAL, REFUND, ADJUSTMENT
    val amountPkr: Double,
    val previousBalance: Double = 0.0,
    val newBalance: Double = 0.0,
    val status: String = "SUCCESSFUL", // SUCCESSFUL, PENDING, FAILED, CANCELLED
    val paymentProvider: String = "", // JAZZCASH, EASYPAISA, GAME_CYBER_CRASH, etc.
    val providerTxnId: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "withdrawals")
data class WithdrawalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "p6692_user_1",
    val withdrawalId: String, // e.g. WTH-JC-849201
    val amountPkr: Double,
    val method: String, // JAZZCASH, EASYPAISA
    val accountTitle: String,
    val accountNumber: String,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED, COMPLETED
    val requestedAt: Long = System.currentTimeMillis(),
    val processedAt: Long = 0L,
    val adminNotes: String = ""
)

@Entity(tableName = "daily_rewards")
data class DailyRewardEntity(
    @PrimaryKey val day: Int,
    val title: String,
    val rewardCredits: Double,
    val rewardPoints: Int,
    val isClaimed: Boolean = false,
    val isCurrentDay: Boolean = false
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val category: String, // PROMO, SYSTEM, VIP, SECURITY
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val adminName: String,
    val action: String,
    val target: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "deposits")
data class DepositEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val orderId: String, // Unique Order Reference (e.g. ORD-JC-20261006-8492)
    val method: String, // "JAZZCASH" or "EASYPAISA"
    val amountPkr: Double,
    val status: String = "PENDING", // "PENDING", "PROCESSING", "SUCCESSFUL", "FAILED", "CANCELLED", "EXPIRED"
    val gatewayTxnId: String = "", // Official Provider Transaction Reference
    val adminNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "jazzcash_config")
data class JazzCashConfigEntity(
    @PrimaryKey val id: String = "primary_config",
    val businessName: String = "Ahmed Shop",
    val accountNumber: String = "01104590863",
    val merchantId: String = "MC895945",
    val apiPassword: String = "z84t9fx8c8",
    val integritySalt: String = "498z5u0th4",
    val returnUrl: String = "https://api.p6692.com/api/jazzcash/callback",
    val environment: String = "PRODUCTION", // "PRODUCTION" or "SANDBOX"
    val epStoreId: String = "38914",
    val epHashKey: String = "ep_live_sec_9941a82f",
    val epAccountTitle: String = "P6692 Official Merchant",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cryptoeze_config")
data class CryptoEzeConfigEntity(
    @PrimaryKey val id: String = "primary_cryptoeze",
    val merchantId: String = "merch_8bbc74b534e28efb54a3de42",
    val apiKey: String = "pk_live_1bbea1f4b9cab098a5189af471d580e9",
    val secretKey: String = "sk_live_3860",
    val webhookSecret: String = "whsec_4a6112",
    val webhookUrl: String = "https://p6692.com/webhooks/cryptoeze",
    val environment: String = "LIVE", // "SANDBOX" or "LIVE"
    val defaultChannel: String = "JAZZCASH", // "JAZZCASH", "EASYPAISA", "USDT"
    val settlementAccount: String = "03001234567",
    val apiUrl: String = "https://api.cryptoeze.com/api/v1",
    val updatedAt: Long = System.currentTimeMillis()
)

