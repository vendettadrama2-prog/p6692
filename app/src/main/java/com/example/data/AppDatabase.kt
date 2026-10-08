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
    version = 4,
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

            // 2. Seed Games Catalogue if empty
            val gamesCount = db.gameDao().getGamesCount()
            if (gamesCount == 0) {
                val games = listOf(
                    GameEntity(
                        id = "cyber_crash",
                        title = "Cyber Crash: Rocket 92",
                        category = "Crash",
                        description = "Thrilling high-stakes rocket ascent in real PKR! Watch the neon multiplier skyrocket up to 5,000x and bail out before terminal warp burn.",
                        rating = 4.95f,
                        playsCount = 428000,
                        isFeatured = true,
                        isPopular = true,
                        isNew = false,
                        minBetPkr = 50.0,
                        maxBetPkr = 25000.0,
                        maxMultiplier = "5,000x",
                        rtpPercent = 98.2f,
                        accentColorHex = "#22D3EE"
                    ),
                    GameEntity(
                        id = "neon_spin",
                        title = "Mega 777 Neon Rush",
                        category = "Slots",
                        description = "Electrifying 3-reel cyberpunk slot machine with cascading wild crystals, neon 7s, and instant PKR win payouts.",
                        rating = 4.88f,
                        playsCount = 312000,
                        isFeatured = true,
                        isPopular = true,
                        isNew = false,
                        minBetPkr = 20.0,
                        maxBetPkr = 10000.0,
                        maxMultiplier = "1,500x",
                        rtpPercent = 97.8f,
                        accentColorHex = "#B56CFF"
                    ),
                    GameEntity(
                        id = "cyber_mines",
                        title = "Grid Hunter: Cyber Mines",
                        category = "Mines",
                        description = "Tactical 5x5 quantum grid. Uncover encrypted cyber cores while evading security glitch mines. Cash out anytime in real PKR!",
                        rating = 4.92f,
                        playsCount = 189000,
                        isFeatured = true,
                        isPopular = false,
                        isNew = true,
                        minBetPkr = 25.0,
                        maxBetPkr = 20000.0,
                        maxMultiplier = "2,400x",
                        rtpPercent = 98.5f,
                        accentColorHex = "#10B981"
                    ),
                    GameEntity(
                        id = "neon_blackjack",
                        title = "Cyber 21: Neon Deck",
                        category = "Cards",
                        description = "High-speed arcade table strategy in real PKR. Go head-to-head with the Neural Dealer. Hit, stand, or double down for cyber supremacy.",
                        rating = 4.82f,
                        playsCount = 145000,
                        isFeatured = false,
                        isPopular = true,
                        isNew = false,
                        minBetPkr = 50.0,
                        maxBetPkr = 15000.0,
                        maxMultiplier = "3x",
                        rtpPercent = 99.1f,
                        accentColorHex = "#FBBF24"
                    ),
                    GameEntity(
                        id = "quantum_roulette",
                        title = "Quantum Neon Wheel",
                        category = "Arcade",
                        description = "Multi-ring orbital spin simulator with quantum speed boosts, cyber sector zones, and instant real PKR multipliers.",
                        rating = 4.79f,
                        playsCount = 98000,
                        isFeatured = false,
                        isPopular = false,
                        isNew = true,
                        minBetPkr = 10.0,
                        maxBetPkr = 10000.0,
                        maxMultiplier = "360x",
                        rtpPercent = 97.3f,
                        accentColorHex = "#EC4899"
                    ),
                    GameEntity(
                        id = "dragon_strike",
                        title = "Dragon Strike 6692",
                        category = "Mini Games",
                        description = "Mythical techno-beast clash featuring hyper combos, plasma blasts, and interactive progressive PKR betting challenges.",
                        rating = 4.85f,
                        playsCount = 204000,
                        isFeatured = true,
                        isPopular = true,
                        isNew = false,
                        minBetPkr = 30.0,
                        maxBetPkr = 12000.0,
                        maxMultiplier = "888x",
                        rtpPercent = 97.6f,
                        accentColorHex = "#F59E0B"
                    ),
                    GameEntity(
                        id = "speed_dice",
                        title = "Cyber Dice 99",
                        category = "Mini Games",
                        description = "Fast-paced probability dice roll. Set your win chance slider from 1% to 95% and place your PKR bet with instant payouts.",
                        rating = 4.87f,
                        playsCount = 167000,
                        isFeatured = false,
                        isPopular = true,
                        isNew = true,
                        minBetPkr = 10.0,
                        maxBetPkr = 25000.0,
                        maxMultiplier = "990x",
                        rtpPercent = 99.0f,
                        accentColorHex = "#3B82F6"
                    ),
                    GameEntity(
                        id = "neon_plinko",
                        title = "Neon Plinko Drop",
                        category = "Mini Games",
                        description = "Physics-based ball drop with 16 rows of neon pegs and extreme outer pockets up to 1,000x real PKR payout.",
                        rating = 4.91f,
                        playsCount = 256000,
                        isFeatured = true,
                        isPopular = true,
                        isNew = false,
                        minBetPkr = 20.0,
                        maxBetPkr = 15000.0,
                        maxMultiplier = "1,000x",
                        rtpPercent = 98.4f,
                        accentColorHex = "#8B5CF6"
                    ),
                    GameEntity(
                        id = "jelly",
                        title = "Jelly: Cluster Rush",
                        category = "Slots",
                        description = "Sweet cascading neon jelly matrix! Drop and match fruit clusters for juicy real PKR multipliers up to 250x.",
                        rating = 4.94f,
                        playsCount = 380000,
                        isFeatured = true,
                        isPopular = true,
                        isNew = true,
                        minBetPkr = 25.0,
                        maxBetPkr = 20000.0,
                        maxMultiplier = "250x",
                        rtpPercent = 98.6f,
                        accentColorHex = "#B56CFF"
                    ),
                    GameEntity(
                        id = "aviator",
                        title = "Aviator: High Ascent",
                        category = "Crash",
                        description = "The iconic red plane high-altitude flight in real PKR! Watch the coefficient rise and cash out before it flies away into the stratosphere.",
                        rating = 4.98f,
                        playsCount = 520000,
                        isFeatured = true,
                        isPopular = true,
                        isNew = true,
                        minBetPkr = 50.0,
                        maxBetPkr = 25000.0,
                        maxMultiplier = "10,000x",
                        rtpPercent = 99.0f,
                        accentColorHex = "#F43F5E"
                    ),
                    GameEntity(
                        id = "mines",
                        title = "Mines: Diamond Grid",
                        category = "Mines",
                        description = "Classic grid-based Mines gameplay with selectable 3x3, 5x5, 6x6 grids, customizable mine counts, and real PKR cashout.",
                        rating = 4.95f,
                        playsCount = 410000,
                        isFeatured = true,
                        isPopular = true,
                        isNew = true,
                        minBetPkr = 25.0,
                        maxBetPkr = 25000.0,
                        maxMultiplier = "5,000x",
                        rtpPercent = 98.8f,
                        accentColorHex = "#10B981"
                    ),
                    GameEntity(
                        id = "plinko",
                        title = "Plinko: Neon Drop",
                        category = "Mini Games",
                        description = "Visually stunning physics-based Plinko pegboard with falling ball trajectories, dynamic risk levels, and up to 29x instant PKR multipliers.",
                        rating = 4.93f,
                        playsCount = 395000,
                        isFeatured = true,
                        isPopular = true,
                        isNew = true,
                        minBetPkr = 25.0,
                        maxBetPkr = 20000.0,
                        maxMultiplier = "29x",
                        rtpPercent = 98.7f,
                        accentColorHex = "#22D3EE"
                    )
                )
                db.gameDao().insertGames(games)

                // Seed Favorites
                db.favoriteDao().addFavorite(FavoriteEntity("p6692_user_1", "cyber_crash"))
                db.favoriteDao().addFavorite(FavoriteEntity("p6692_user_1", "neon_spin"))
                db.favoriteDao().addFavorite(FavoriteEntity("p6692_user_1", "aviator"))
                db.favoriteDao().addFavorite(FavoriteEntity("p6692_user_1", "jelly"))
                db.favoriteDao().addFavorite(FavoriteEntity("p6692_user_1", "mines"))
                db.favoriteDao().addFavorite(FavoriteEntity("p6692_user_1", "plinko"))
            } else {
                // Ensure Jelly, Aviator, Mines, and Plinko are always present in the existing database
                val jellyExisting = db.gameDao().getGameById("jelly")
                if (jellyExisting == null) {
                    db.gameDao().insertGames(
                        listOf(
                            GameEntity(
                                id = "jelly",
                                title = "Jelly: Cluster Rush",
                                category = "Slots",
                                description = "Sweet cascading neon jelly matrix! Drop and match fruit clusters for juicy real PKR multipliers up to 250x.",
                                rating = 4.94f,
                                playsCount = 380000,
                                isFeatured = true,
                                isPopular = true,
                                isNew = true,
                                minBetPkr = 25.0,
                                maxBetPkr = 20000.0,
                                maxMultiplier = "250x",
                                rtpPercent = 98.6f,
                                accentColorHex = "#B56CFF"
                            )
                        )
                    )
                }
                val aviatorExisting = db.gameDao().getGameById("aviator")
                if (aviatorExisting == null) {
                    db.gameDao().insertGames(
                        listOf(
                            GameEntity(
                                id = "aviator",
                                title = "Aviator: High Ascent",
                                category = "Crash",
                                description = "The iconic red plane high-altitude flight in real PKR! Watch the coefficient rise and cash out before it flies away into the stratosphere.",
                                rating = 4.98f,
                                playsCount = 520000,
                                isFeatured = true,
                                isPopular = true,
                                isNew = true,
                                minBetPkr = 50.0,
                                maxBetPkr = 25000.0,
                                maxMultiplier = "10,000x",
                                rtpPercent = 99.0f,
                                accentColorHex = "#F43F5E"
                            )
                        )
                    )
                }
                val minesExisting = db.gameDao().getGameById("mines")
                if (minesExisting == null) {
                    db.gameDao().insertGames(
                        listOf(
                            GameEntity(
                                id = "mines",
                                title = "Mines: Diamond Grid",
                                category = "Mines",
                                description = "Classic grid-based Mines gameplay with selectable 3x3, 5x5, 6x6 grids, customizable mine counts, and real PKR cashout.",
                                rating = 4.95f,
                                playsCount = 410000,
                                isFeatured = true,
                                isPopular = true,
                                isNew = true,
                                minBetPkr = 25.0,
                                maxBetPkr = 25000.0,
                                maxMultiplier = "5,000x",
                                rtpPercent = 98.8f,
                                accentColorHex = "#10B981"
                            )
                        )
                    )
                }
                val plinkoExisting = db.gameDao().getGameById("plinko")
                if (plinkoExisting == null) {
                    db.gameDao().insertGames(
                        listOf(
                            GameEntity(
                                id = "plinko",
                                title = "Plinko: Neon Drop",
                                category = "Mini Games",
                                description = "Visually stunning physics-based Plinko pegboard with falling ball trajectories, dynamic risk levels, and up to 29x instant PKR multipliers.",
                                rating = 4.93f,
                                playsCount = 395000,
                                isFeatured = true,
                                isPopular = true,
                                isNew = true,
                                minBetPkr = 25.0,
                                maxBetPkr = 20000.0,
                                maxMultiplier = "29x",
                                rtpPercent = 98.7f,
                                accentColorHex = "#22D3EE"
                            )
                        )
                    )
                }
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
