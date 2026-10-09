package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        GameEntity::class,
        FavoriteEntity::class,
        GameHistoryEntity::class,
        TransactionEntity::class,
        DailyRewardEntity::class,
        NotificationEntity::class,
        AuditLogEntity::class,
        DepositEntity::class,
        WithdrawalEntity::class,
        JazzCashConfigEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun gameDao(): GameDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun gameHistoryDao(): GameHistoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun withdrawalDao(): WithdrawalDao
    abstract fun dailyRewardDao(): DailyRewardDao
    abstract fun notificationDao(): NotificationDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun depositDao(): DepositDao
    abstract fun jazzCashConfigDao(): JazzCashConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "p6692_master_database.db"
                ).fallbackToDestructiveMigration(true)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                // Proactively trigger seed check on dispatchers IO
                scope.launch(Dispatchers.IO) {
                    populateInitialData(instance)
                }
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            // 1. Seed or Verify User with Real PKR Balance
            val existingUser = db.userDao().getUserByIdSync("p6692_user_1")
            if (existingUser == null) {
                db.userDao().insertUser(
                    UserEntity(
                        id = "p6692_user_1",
                        username = "CyberKnight_92",
                        email = "player@p6692.com",
                        vipLevel = 3,
                        pkrBalance = 25000.0, // Default Real PKR balance: PKR 25,000
                        demoBalance = 25000.0,
                        rewardPoints = 4850,
                        role = "PLAYER"
                    )
                )
            } else if (existingUser.pkrBalance <= 0.0) {
                db.userDao().setPkrBalance("p6692_user_1", 25000.0)
            }

            // 2. Seed / Upsert all canonical games to ensure all games (Fortune Gems 2, Spin Master Spain, Rocket 92 Aviator, etc.) exist and are enabled
            for (game in CANONICAL_GAMES) {
                val existing = db.gameDao().getGameById(game.id)
                if (existing == null) {
                    db.gameDao().insertGames(listOf(game))
                } else {
                    db.gameDao().setGameEnabled(game.id, true)
                }
            }

            // Seed Initial Favorites
            listOf(
                "fortune_gems",
                "aviator",
                "money_coming",
                "cyber_crash",
                "neon_spin",
                "jelly",
                "mines",
                "plinko",
                "bubble_shooter",
                "candy_match",
                "fruit_merge"
            ).forEach { gameId ->
                db.favoriteDao().addFavorite(FavoriteEntity("p6692_user_1", gameId))
            }

            // 3. Seed Daily Rewards if empty
            val dailyRewards = listOf(
                DailyRewardEntity(1, "Day 1 Welcome", 1000.0, 100, isClaimed = true),
                DailyRewardEntity(2, "Day 2 Boost", 1500.0, 150, isClaimed = true),
                DailyRewardEntity(3, "Day 3 Super Charge", 2500.0, 250, isClaimed = false, isCurrentDay = true),
                DailyRewardEntity(4, "Day 4 Plasma Key", 3500.0, 350, isClaimed = false),
                DailyRewardEntity(5, "Day 5 Matrix Vault", 5000.0, 500, isClaimed = false),
                DailyRewardEntity(6, "Day 6 Cyber Core", 7500.0, 750, isClaimed = false),
                DailyRewardEntity(7, "Day 7 Grand Jackpot Box", 15000.0, 1500, isClaimed = false)
            )
            db.dailyRewardDao().insertRewards(dailyRewards)

            // 4. Seed Initial Wallet Ledger Transactions
            val existingTxns = db.transactionDao().getTransactions("p6692_user_1")
            // Insert seed welcome deposit record if none exists
            db.transactionDao().insertTransaction(
                TransactionEntity(
                    userId = "p6692_user_1",
                    transactionId = "TXN-DEP-WELCOME-2026",
                    type = "DEPOSIT",
                    amountPkr = 25000.0,
                    previousBalance = 0.0,
                    newBalance = 25000.0,
                    status = "SUCCESSFUL",
                    paymentProvider = "JAZZCASH",
                    providerTxnId = "JC998241029",
                    description = "Welcome Account Credit via JazzCash Gateway"
                )
            )

            // 5. Seed Initial Notifications
            val notifs = listOf(
                NotificationEntity(
                    title = "⚡ Welcome to P6692 Real PKR Gaming",
                    message = "Your account is active with PKR 25,000 initial balance. Place real PKR bets on Cyber Crash, Neon Slots, and Cyber Mines!",
                    category = "SYSTEM"
                ),
                NotificationEntity(
                    title = "💳 JazzCash & Easypaisa Gateways Online",
                    message = "Official 256-bit SSL online payment checkout is configured for instant PKR deposits and withdrawals.",
                    category = "PROMO"
                )
            )
            for (n in notifs) {
                db.notificationDao().insertNotification(n)
            }

            // 6. Seed JazzCash Business Configuration
            db.jazzCashConfigDao().saveConfig(
                JazzCashConfigEntity(
                    id = "primary_config",
                    businessName = "Ahmed Shop",
                    accountNumber = "01104590863",
                    merchantId = "MC895945",
                    apiPassword = "z84t9fx8c8",
                    integritySalt = "498z5u0th4"
                )
            )
        }
    }
}
