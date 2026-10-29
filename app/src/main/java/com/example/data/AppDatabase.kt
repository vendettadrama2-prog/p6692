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
        JazzCashConfigEntity::class,
        CryptoEzeConfigEntity::class
    ],
    version = 13,
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
    abstract fun cryptoEzeConfigDao(): CryptoEzeConfigDao

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
            // 1. Seed or Verify User with Real PKR Balance (0.00) & Demo Balance (5,000.00)
            val existingUser = db.userDao().getUserByIdSync("p6692_user_1")
            if (existingUser == null) {
                db.userDao().insertUser(
                    UserEntity(
                        id = "p6692_user_1",
                        username = "CyberKnight_92",
                        mobileNumber = "03001234567",
                        email = "player@p6692.com",
                        vipLevel = 1,
                        pkrBalance = 0.0, // Real PKR Wallet Balance starts at PKR 0.00
                        demoBalance = 5000.0, // Initial DEMO balance: PKR 5,000 (DEMO BALANCE — NOT REAL MONEY)
                        rewardPoints = 500,
                        role = "PLAYER"
                    )
                )
            }

            // 2. Seed / Upsert all canonical games (Car Racing, Ludo, Subway Runner, Piano Tiles, Fortune Gems 2, etc.)
            db.gameDao().insertGames(CANONICAL_GAMES)

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
                DailyRewardEntity(1, "Day 1 Welcome", 100.0, 50, isClaimed = true),
                DailyRewardEntity(2, "Day 2 Boost", 150.0, 75, isClaimed = true),
                DailyRewardEntity(3, "Day 3 Super Charge", 250.0, 100, isClaimed = false, isCurrentDay = true),
                DailyRewardEntity(4, "Day 4 Plasma Key", 350.0, 150, isClaimed = false),
                DailyRewardEntity(5, "Day 5 Matrix Vault", 500.0, 200, isClaimed = false),
                DailyRewardEntity(6, "Day 6 Cyber Core", 750.0, 300, isClaimed = false),
                DailyRewardEntity(7, "Day 7 Grand Jackpot Box", 1500.0, 500, isClaimed = false)
            )
            db.dailyRewardDao().insertRewards(dailyRewards)

            // 4. Seed Initial Notifications
            val notifs = listOf(
                NotificationEntity(
                    title = "⚡ Welcome to P6692 Gaming",
                    message = "Your account is active. Start with PKR 5,000 DEMO practice credits or deposit real PKR via official JazzCash gateway!",
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

            // 5. Seed JazzCash Business Configuration
            db.jazzCashConfigDao().saveConfig(
                JazzCashConfigEntity(
                    id = "primary_config",
                    businessName = "Ahmed Shop",
                    accountNumber = "01104590863",
                    merchantId = "MC895945",
                    apiPassword = "z84t9fx8c8",
                    integritySalt = "498z5u0th4",
                    returnUrl = "https://api.p6692.com/api/jazzcash/callback",
                    environment = "PRODUCTION"
                )
            )

            // 6. Seed CryptoEze Gateway Configuration (routing to JazzCash)
            db.cryptoEzeConfigDao().saveConfig(
                CryptoEzeConfigEntity(
                    id = "primary_cryptoeze",
                    merchantId = "merch_8bbc74b534e28efb54a3de42",
                    apiKey = "pk_live_1bbea1f4b9cab098a5189af471d580e9",
                    secretKey = "sk_live_3860",
                    webhookSecret = "whsec_4a6112",
                    webhookUrl = "https://p6692.com/webhooks/cryptoeze",
                    environment = "LIVE",
                    defaultChannel = "JAZZCASH",
                    settlementAccount = "03001234567",
                    apiUrl = "https://cryptoeze.com/api/v1"
                )
            )
        }
    }
}
