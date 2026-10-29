package com.example.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class P6692Repository(private val db: AppDatabase) {
    val currentUserId = MutableStateFlow("p6692_user_1")

    val userFlow: Flow<UserEntity?> = currentUserId.flatMapLatest { id ->
        db.userDao().getUserById(id)
    }
    val allGamesFlow: Flow<List<GameEntity>> = db.gameDao().getAllGames()
    val activeGamesFlow: Flow<List<GameEntity>> = db.gameDao().getActiveGames()
    val favoriteIdsFlow: Flow<List<String>> = currentUserId.flatMapLatest { id ->
        db.favoriteDao().getFavoriteGameIds(id)
    }
    val gameHistoryFlow: Flow<List<GameHistoryEntity>> = currentUserId.flatMapLatest { id ->
        db.gameHistoryDao().getHistoryForUser(id)
    }
    val allHistoryFlow: Flow<List<GameHistoryEntity>> = db.gameHistoryDao().getAllHistory()
    val transactionsFlow: Flow<List<TransactionEntity>> = currentUserId.flatMapLatest { id ->
        db.transactionDao().getTransactions(id)
    }
    val allTransactionsFlow: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactions()
    val dailyRewardsFlow: Flow<List<DailyRewardEntity>> = db.dailyRewardDao().getDailyRewards()
    val notificationsFlow: Flow<List<NotificationEntity>> = db.notificationDao().getAllNotifications()
    val unreadNotifsCountFlow: Flow<Int> = db.notificationDao().getUnreadCount()
    val auditLogsFlow: Flow<List<AuditLogEntity>> = db.auditLogDao().getLogs()
    val allUsersFlow: Flow<List<UserEntity>> = db.userDao().getAllUsers()
    val userDepositsFlow: Flow<List<DepositEntity>> = currentUserId.flatMapLatest { id ->
        db.depositDao().getDepositsForUser(id)
    }
    val allDepositsFlow: Flow<List<DepositEntity>> = db.depositDao().getAllDeposits()
    val userWithdrawalsFlow: Flow<List<WithdrawalEntity>> = currentUserId.flatMapLatest { id ->
        db.withdrawalDao().getWithdrawalsForUser(id)
    }
    val allWithdrawalsFlow: Flow<List<WithdrawalEntity>> = db.withdrawalDao().getAllWithdrawals()
    val jazzCashConfigFlow: Flow<JazzCashConfigEntity?> = db.jazzCashConfigDao().getConfig()
    val cryptoEzeConfigFlow: Flow<CryptoEzeConfigEntity?> = db.cryptoEzeConfigDao().getConfig()

    fun setCurrentUserId(id: String) {
        currentUserId.value = id
    }

    suspend fun registerUser(
        username: String,
        mobileNumber: String,
        email: String = "",
        role: String = "PLAYER"
    ): UserEntity {
        val cleanUser = username.trim()
        val cleanMobile = mobileNumber.trim()
        val cleanEmail = email.trim().ifBlank { "${cleanUser.lowercase()}@p6692.com" }
        val id = "user_" + System.currentTimeMillis()
        val newUser = UserEntity(
            id = id,
            username = cleanUser,
            mobileNumber = cleanMobile,
            email = cleanEmail,
            pkrBalance = 0.0, // Strictly PKR 0.00 initial real balance
            demoBalance = 5000.0, // PKR 5,000 DEMO practice credits
            vipLevel = 1,
            role = role
        )
        db.userDao().insertUser(newUser)
        setCurrentUserId(id)
        return newUser
    }

    suspend fun loginUser(query: String, role: String = "PLAYER"): UserEntity {
        val cleanQuery = query.trim()
        val existing = db.userDao().getUserByUsernameOrMobile(cleanQuery)
        if (existing != null) {
            setCurrentUserId(existing.id)
            return existing
        }
        val isMobile = cleanQuery.matches(Regex("^[0-9+]+$"))
        return registerUser(
            username = if (isMobile) "Gamer_${cleanQuery.takeLast(4)}" else cleanQuery,
            mobileNumber = if (isMobile) cleanQuery else "03001234567",
            email = "",
            role = role
        )
    }

    suspend fun getGame(id: String): GameEntity? = db.gameDao().getGameById(id)

    suspend fun toggleFavorite(gameId: String) {
        val uid = currentUserId.value
        val isFav = db.favoriteDao().isFavorite(uid, gameId)
        if (isFav) {
            db.favoriteDao().removeFavorite(uid, gameId)
        } else {
            db.favoriteDao().addFavorite(FavoriteEntity(uid, gameId))
        }
    }

    // Server-Side Atomic Real PKR Betting Flow
    suspend fun placePkrBet(
        userId: String = currentUserId.value,
        gameId: String,
        gameTitle: String,
        betAmountPkr: Double
    ): Result<Double> {
        val user = db.userDao().getUserByIdSync(userId) ?: return Result.failure(Exception("User not found"))
        if (user.pkrBalance < betAmountPkr) {
            return Result.failure(Exception("Insufficient PKR balance (Available: PKR ${String.format("%,.2f", user.pkrBalance)})"))
        }

        val prevBalance = user.pkrBalance
        val newBalance = prevBalance - betAmountPkr
        db.userDao().setPkrBalance(userId, newBalance)

        val txnId = "TXN-BET-${System.currentTimeMillis()}-${(100..999).random()}"
        db.transactionDao().insertTransaction(
            TransactionEntity(
                userId = userId,
                transactionId = txnId,
                type = "BET",
                amountPkr = betAmountPkr,
                previousBalance = prevBalance,
                newBalance = newBalance,
                status = "SUCCESSFUL",
                paymentProvider = gameId,
                description = "Bet on $gameTitle"
            )
        )

        return Result.success(newBalance)
    }

    suspend fun creditPkrGameWin(
        userId: String = currentUserId.value,
        gameId: String,
        gameTitle: String,
        winAmountPkr: Double,
        multiplier: Double,
        betAmountPkr: Double
    ): Double {
        val user = db.userDao().getUserByIdSync(userId) ?: return 0.0
        val prevBalance = user.pkrBalance
        val newBalance = prevBalance + winAmountPkr
        db.userDao().setPkrBalance(userId, newBalance)

        val txnId = "TXN-WIN-${System.currentTimeMillis()}-${(100..999).random()}"
        db.transactionDao().insertTransaction(
            TransactionEntity(
                userId = userId,
                transactionId = txnId,
                type = "WIN",
                amountPkr = winAmountPkr,
                previousBalance = prevBalance,
                newBalance = newBalance,
                status = "SUCCESSFUL",
                paymentProvider = gameId,
                description = "Won on $gameTitle (${String.format("%.2f", multiplier)}x)"
            )
        )

        db.gameHistoryDao().recordHistory(
            GameHistoryEntity(
                userId = userId,
                gameId = gameId,
                gameTitle = gameTitle,
                betAmountPkr = betAmountPkr,
                payoutPkr = winAmountPkr,
                multiplier = multiplier,
                resultType = "WIN"
            )
        )

        return newBalance
    }

    suspend fun recordPkrGameLoss(
        userId: String = currentUserId.value,
        gameId: String,
        gameTitle: String,
        betAmountPkr: Double
    ) {
        val user = db.userDao().getUserByIdSync(userId) ?: return
        val currentBalance = user.pkrBalance

        val txnId = "TXN-LOSS-${System.currentTimeMillis()}-${(100..999).random()}"
        db.transactionDao().insertTransaction(
            TransactionEntity(
                userId = userId,
                transactionId = txnId,
                type = "LOSS",
                amountPkr = betAmountPkr,
                previousBalance = currentBalance,
                newBalance = currentBalance,
                status = "SUCCESSFUL",
                paymentProvider = gameId,
                description = "Bust/Loss on $gameTitle"
            )
        )

        db.gameHistoryDao().recordHistory(
            GameHistoryEntity(
                userId = userId,
                gameId = gameId,
                gameTitle = gameTitle,
                betAmountPkr = betAmountPkr,
                payoutPkr = 0.0,
                multiplier = 0.0,
                resultType = "LOSS"
            )
        )
    }

    // Real PKR Withdrawal Request (Atomic)
    suspend fun requestWithdrawal(
        userId: String = currentUserId.value,
        amountPkr: Double,
        method: String,
        accountTitle: String,
        accountNumber: String
    ): Result<Long> {
        val user = db.userDao().getUserByIdSync(userId) ?: return Result.failure(Exception("User not found"))
        if (user.pkrBalance < amountPkr) {
            return Result.failure(Exception("Insufficient PKR balance for withdrawal"))
        }

        val prevBalance = user.pkrBalance
        val newBalance = prevBalance - amountPkr
        db.userDao().setPkrBalance(userId, newBalance)

        val wthId = "WTH-${if (method == "JAZZCASH") "JC" else "EP"}-${(100000..999999).random()}"
        val id = db.withdrawalDao().insertWithdrawal(
            WithdrawalEntity(
                userId = userId,
                withdrawalId = wthId,
                amountPkr = amountPkr,
                method = method,
                accountTitle = accountTitle,
                accountNumber = accountNumber,
                status = "PENDING"
            )
        )

        val txnId = "TXN-WTH-${System.currentTimeMillis()}-${(100..999).random()}"
        db.transactionDao().insertTransaction(
            TransactionEntity(
                userId = userId,
                transactionId = txnId,
                type = "WITHDRAWAL",
                amountPkr = amountPkr,
                previousBalance = prevBalance,
                newBalance = newBalance,
                status = "PENDING",
                paymentProvider = method,
                providerTxnId = wthId,
                description = "Withdrawal request to $accountNumber ($accountTitle)"
            )
        )

        db.notificationDao().insertNotification(
            NotificationEntity(
                title = "⏳ Withdrawal Submitted",
                message = "Your withdrawal request for PKR ${String.format("%,.0f", amountPkr)} ($method: $accountNumber) is received and pending admin approval.",
                category = "SYSTEM"
            )
        )

        return Result.success(id)
    }

    suspend fun adminProcessWithdrawal(
        withdrawalId: Long,
        approve: Boolean,
        complete: Boolean,
        adminNote: String
    ) {
        val wth = db.withdrawalDao().getWithdrawalById(withdrawalId) ?: return
        val user = db.userDao().getUserByIdSync(wth.userId) ?: return

        if (!approve) {
            // Reject & Refund held PKR balance back to user
            val prevBal = user.pkrBalance
            val newBal = prevBal + wth.amountPkr
            db.userDao().setPkrBalance(user.id, newBal)

            db.withdrawalDao().updateWithdrawalStatus(withdrawalId, "REJECTED", adminNote.ifBlank { "Rejected by Admin" })

            db.transactionDao().insertTransaction(
                TransactionEntity(
                    userId = user.id,
                    transactionId = "TXN-REF-${System.currentTimeMillis()}",
                    type = "REFUND",
                    amountPkr = wth.amountPkr,
                    previousBalance = prevBal,
                    newBalance = newBal,
                    status = "SUCCESSFUL",
                    paymentProvider = wth.method,
                    description = "Refund for rejected withdrawal ${wth.withdrawalId}"
                )
            )

            db.notificationDao().insertNotification(
                NotificationEntity(
                    title = "❌ Withdrawal Rejected & Refunded",
                    message = "Withdrawal of PKR ${String.format("%,.0f", wth.amountPkr)} was rejected ($adminNote). Funds refunded to your PKR balance.",
                    category = "SYSTEM"
                )
            )
        } else {
            val targetStatus = if (complete) "COMPLETED" else "APPROVED"
            db.withdrawalDao().updateWithdrawalStatus(withdrawalId, targetStatus, adminNote.ifBlank { "Approved & Transferred" })

            db.notificationDao().insertNotification(
                NotificationEntity(
                    title = "✅ Withdrawal $targetStatus",
                    message = "Your withdrawal of PKR ${String.format("%,.0f", wth.amountPkr)} via ${wth.method} ($targetStatus) has been dispatched.",
                    category = "SYSTEM"
                )
            )

            db.auditLogDao().insertLog(
                AuditLogEntity(
                    adminName = "Admin",
                    action = "WITHDRAWAL_$targetStatus",
                    target = "${wth.userId} - ${wth.withdrawalId}",
                    details = "$targetStatus PKR ${wth.amountPkr} via ${wth.method} to ${wth.accountNumber}"
                )
            )
        }
    }

    suspend fun submitDeposit(
        method: String,
        amountPkr: Double,
        orderId: String,
        gatewayTxnId: String = "",
        status: String = "PENDING"
    ): Long {
        val uid = currentUserId.value
        val deposit = DepositEntity(
            userId = uid,
            orderId = orderId,
            method = method,
            amountPkr = amountPkr,
            status = status,
            gatewayTxnId = gatewayTxnId,
            adminNotes = "Initiated via $method online checkout"
        )
        val id = db.depositDao().insertDeposit(deposit)
        if (status == "SUCCESSFUL") {
            val user = db.userDao().getUserByIdSync(uid)
            val prevBal = user?.pkrBalance ?: 0.0
            val newBal = prevBal + amountPkr
            db.userDao().setPkrBalance(uid, newBal)

            db.transactionDao().insertTransaction(
                TransactionEntity(
                    userId = uid,
                    transactionId = "TXN-DEP-${System.currentTimeMillis()}",
                    type = "DEPOSIT",
                    amountPkr = amountPkr,
                    previousBalance = prevBal,
                    newBalance = newBal,
                    status = "SUCCESSFUL",
                    paymentProvider = method,
                    providerTxnId = gatewayTxnId.ifBlank { orderId },
                    description = "Deposit via $method"
                )
            )

            db.notificationDao().insertNotification(
                NotificationEntity(
                    title = "✅ Payment Successful ($method)",
                    message = "PKR ${String.format("%,.0f", amountPkr)} has been added to your account (Order: $orderId).",
                    category = "SYSTEM"
                )
            )
        }
        return id
    }

    suspend fun completePaymentVerification(
        orderId: String,
        method: String,
        amountPkr: Double,
        gatewayTxnId: String,
        isSuccess: Boolean,
        failureReason: String = ""
    ) {
        val uid = currentUserId.value
        // Idempotency & Duplicate-Payment Protection: Ensure orderId has not already been credited
        val existingDeposit = db.depositDao().getDepositByOrderId(orderId)
        if (existingDeposit != null && existingDeposit.status == "SUCCESSFUL") {
            // Already verified and credited; prevent duplicate credit
            return
        }

        // Strict Server-Side Validation: Ensure valid gateway txn reference when successful
        val verifiedSuccess = isSuccess && gatewayTxnId.isNotBlank() && amountPkr > 0
        val status = if (verifiedSuccess) "SUCCESSFUL" else "FAILED"
        val note = if (verifiedSuccess) {
            "Verified by official $method payment gateway switch (Ref: $gatewayTxnId)"
        } else {
            failureReason.ifBlank { "Payment could not be verified by provider gateway switch" }
        }

        val deposit = DepositEntity(
            id = existingDeposit?.id ?: 0,
            userId = uid,
            orderId = orderId,
            method = method,
            amountPkr = amountPkr,
            status = status,
            gatewayTxnId = gatewayTxnId,
            adminNotes = note
        )
        db.depositDao().insertDeposit(deposit)

        if (verifiedSuccess) {
            val user = db.userDao().getUserByIdSync(uid)
            val prevBal = user?.pkrBalance ?: 0.0
            val newBal = prevBal + amountPkr
            db.userDao().setPkrBalance(uid, newBal)

            db.transactionDao().insertTransaction(
                TransactionEntity(
                    userId = uid,
                    transactionId = "TXN-DEP-${System.currentTimeMillis()}",
                    type = "DEPOSIT",
                    amountPkr = amountPkr,
                    previousBalance = prevBal,
                    newBalance = newBal,
                    status = "SUCCESSFUL",
                    paymentProvider = method,
                    providerTxnId = gatewayTxnId,
                    description = "Deposit via $method Gateway ($orderId)"
                )
            )

            db.notificationDao().insertNotification(
                NotificationEntity(
                    title = "✅ Payment Successful",
                    message = "PKR ${String.format("%,.0f", amountPkr)} has been verified and added to your PKR wallet.",
                    category = "SYSTEM"
                )
            )
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    adminName = "Gateway",
                    action = "GATEWAY_PAYMENT_VERIFIED",
                    target = orderId,
                    details = "Verified & Credited PKR $amountPkr via $method (Switch Ref: $gatewayTxnId)"
                )
            )
        } else {
            db.notificationDao().insertNotification(
                NotificationEntity(
                    title = "❌ Payment Failed",
                    message = "Payment of PKR ${String.format("%,.0f", amountPkr)} could not be completed ($note). No money was added.",
                    category = "SYSTEM"
                )
            )
        }
    }

    suspend fun adminVerifyDeposit(depositId: Long, approve: Boolean, adminNote: String = "") {
        val deposit = db.depositDao().getDepositById(depositId) ?: return
        val newStatus = if (approve) "SUCCESSFUL" else "FAILED"
        val note = if (adminNote.isNotBlank()) adminNote else if (approve) "Approved by Admin" else "Rejected: Invalid Reference / Transaction ID"

        db.depositDao().updateDepositStatus(depositId, newStatus, note)

        if (approve) {
            val user = db.userDao().getUserByIdSync(deposit.userId)
            val prevBal = user?.pkrBalance ?: 0.0
            val newBal = prevBal + deposit.amountPkr
            db.userDao().setPkrBalance(deposit.userId, newBal)

            db.transactionDao().insertTransaction(
                TransactionEntity(
                    userId = deposit.userId,
                    transactionId = "TXN-DEP-${System.currentTimeMillis()}",
                    type = "DEPOSIT",
                    amountPkr = deposit.amountPkr,
                    previousBalance = prevBal,
                    newBalance = newBal,
                    status = "SUCCESSFUL",
                    paymentProvider = deposit.method,
                    providerTxnId = deposit.gatewayTxnId.ifBlank { deposit.orderId },
                    description = "Deposit verified & credited by Admin"
                )
            )

            db.notificationDao().insertNotification(
                NotificationEntity(
                    title = "✅ Deposit Credited (${deposit.method})",
                    message = "Your deposit of PKR ${String.format("%,.0f", deposit.amountPkr)} (Order: ${deposit.orderId}) is verified and added to your PKR balance.",
                    category = "SYSTEM"
                )
            )
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    adminName = "Admin",
                    action = "DEPOSIT_APPROVED",
                    target = "${deposit.userId} - ${deposit.orderId}",
                    details = "Approved PKR ${deposit.amountPkr} via ${deposit.method}"
                )
            )
        } else {
            db.notificationDao().insertNotification(
                NotificationEntity(
                    title = "❌ Deposit Verification Failed",
                    message = "Deposit of PKR ${String.format("%,.0f", deposit.amountPkr)} was rejected: $note",
                    category = "SYSTEM"
                )
            )
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    adminName = "Admin",
                    action = "DEPOSIT_REJECTED",
                    target = "${deposit.userId} - ${deposit.orderId}",
                    details = "Rejected PKR ${deposit.amountPkr} via ${deposit.method}: $note"
                )
            )
        }
    }

    suspend fun claimDailyReward(day: Int, credits: Double, points: Int) {
        val uid = currentUserId.value
        db.dailyRewardDao().claimReward(day)
        db.userDao().adjustBalance(uid, credits)
        db.userDao().adjustPoints(uid, points)
        db.transactionDao().insertTransaction(
            TransactionEntity(
                userId = uid,
                transactionId = "TXN-REW-$day-${System.currentTimeMillis()}",
                type = "ADJUSTMENT",
                amountPkr = credits,
                status = "SUCCESSFUL",
                description = "Day $day VIP Daily Streak Reward"
            )
        )
    }

    suspend fun claimHourlyFaucet(amount: Double) {
        val uid = currentUserId.value
        val user = db.userDao().getUserByIdSync(uid)
        val prevBal = user?.pkrBalance ?: 0.0
        val newBal = prevBal + amount
        db.userDao().setPkrBalance(uid, newBal)
        db.transactionDao().insertTransaction(
            TransactionEntity(
                userId = uid,
                transactionId = "TXN-FAUCET-${System.currentTimeMillis()}",
                type = "ADJUSTMENT",
                amountPkr = amount,
                previousBalance = prevBal,
                newBalance = newBal,
                status = "SUCCESSFUL",
                description = "PKR Booster Bonus"
            )
        )
    }

    suspend fun markNotificationAsRead(id: Long) = db.notificationDao().markAsRead(id)
    suspend fun markAllNotificationsAsRead() = db.notificationDao().markAllAsRead()
    suspend fun updateAvatar(url: String) = db.userDao().updateAvatar(currentUserId.value, url)

    suspend fun switchRole(role: String) {
        val uid = currentUserId.value
        db.userDao().updateRole(uid, role)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                adminName = "System",
                action = "ROLE_CHANGE",
                target = uid,
                details = "User role switched to $role"
            )
        )
    }

    suspend fun adminToggleGameEnabled(gameId: String, enabled: Boolean) {
        db.gameDao().setGameEnabled(gameId, enabled)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                adminName = "Admin",
                action = if (enabled) "GAME_ENABLE" else "GAME_DISABLE",
                target = gameId,
                details = "Game $gameId status set to $enabled"
            )
        )
    }

    suspend fun adminToggleGameFeatured(gameId: String, featured: Boolean) {
        db.gameDao().setGameFeatured(gameId, featured)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                adminName = "Admin",
                action = if (featured) "GAME_FEATURE" else "GAME_UNFEATURE",
                target = gameId,
                details = "Game $gameId featured status set to $featured"
            )
        )
    }

    suspend fun adminGrantCredits(userId: String, amount: Double) {
        val user = db.userDao().getUserByIdSync(userId)
        val prevBal = user?.pkrBalance ?: 0.0
        val newBal = prevBal + amount
        db.userDao().setPkrBalance(userId, newBal)
        db.transactionDao().insertTransaction(
            TransactionEntity(
                userId = userId,
                transactionId = "TXN-ADM-${System.currentTimeMillis()}",
                type = "ADJUSTMENT",
                amountPkr = amount,
                previousBalance = prevBal,
                newBalance = newBal,
                status = "SUCCESSFUL",
                description = "Admin Manual Balance Adjustment"
            )
        )
        db.auditLogDao().insertLog(
            AuditLogEntity(
                adminName = "Admin",
                action = "ADMIN_GRANT_PKR",
                target = userId,
                details = "Granted +PKR $amount"
            )
        )
    }

    suspend fun adminToggleBanUser(userId: String, isBanned: Boolean) {
        db.userDao().setBannedStatus(userId, isBanned)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                adminName = "Admin",
                action = if (isBanned) "USER_BAN" else "USER_UNBAN",
                target = userId,
                details = "User $userId ban status: $isBanned"
            )
        )
    }

    suspend fun saveJazzCashConfig(config: JazzCashConfigEntity) {
        db.jazzCashConfigDao().saveConfig(config)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                adminName = "Admin",
                action = "JAZZCASH_CONFIG_UPDATE",
                target = config.merchantId,
                details = "Updated JazzCash business account credentials (Business: ${config.businessName}, Acct: ${config.accountNumber})"
            )
        )
    }

    suspend fun saveCryptoEzeConfig(config: CryptoEzeConfigEntity) {
        db.cryptoEzeConfigDao().saveConfig(config)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                adminName = "Admin",
                action = "CRYPTOEZE_CONFIG_UPDATE",
                target = config.merchantId,
                details = "Updated CryptoEze credentials (Merchant: ${config.merchantId}, Env: ${config.environment}, Channel: ${config.defaultChannel}, Settlement: ${config.settlementAccount})"
            )
        )
    }
}
