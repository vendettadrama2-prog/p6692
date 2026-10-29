package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.components.NavScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class P6692ViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = P6692Repository(database)

    val user: StateFlow<UserEntity?> = repository.userFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeGames: StateFlow<List<GameEntity>> = repository.activeGamesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGames: StateFlow<List<GameEntity>> = repository.allGamesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<List<String>> = repository.favoriteIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gameHistory: StateFlow<List<GameHistoryEntity>> = repository.gameHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHistory: StateFlow<List<GameHistoryEntity>> = repository.allHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyRewards: StateFlow<List<DailyRewardEntity>> = repository.dailyRewardsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.notificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotifsCount: StateFlow<Int> = repository.unreadNotifsCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userDeposits: StateFlow<List<DepositEntity>> = repository.userDepositsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDeposits: StateFlow<List<DepositEntity>> = repository.allDepositsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userWithdrawals: StateFlow<List<WithdrawalEntity>> = repository.userWithdrawalsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWithdrawals: StateFlow<List<WithdrawalEntity>> = repository.allWithdrawalsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val jazzCashConfig: StateFlow<JazzCashConfigEntity?> = repository.jazzCashConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val cryptoEzeConfig: StateFlow<CryptoEzeConfigEntity?> = repository.cryptoEzeConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Authentication & Startup Gateway State
    var isLoggedIn = MutableStateFlow(false)

    // Navigation and Modals State
    var currentNavScreen = MutableStateFlow(NavScreen.HOME)
    var activeGameForPlay = MutableStateFlow<GameEntity?>(null)
    var selectedGameForDetail = MutableStateFlow<GameEntity?>(null)
    var showAiAdvisor = MutableStateFlow(false)
    var aiAdvisorGameTitle = MutableStateFlow("Cyber Crash: Rocket 92")
    var customAvatarBitmap = MutableStateFlow<Bitmap?>(null)
    var soundEnabled = MutableStateFlow(true)
    var showAuthModal = MutableStateFlow(false)
    var showAdminDashboard = MutableStateFlow(false)
    var showNotificationsModal = MutableStateFlow(false)
    var showDepositModal = MutableStateFlow(false)
    var showWithdrawalModal = MutableStateFlow(false)

    fun onUserLoggedIn(userIdentifier: String, role: String) {
        viewModelScope.launch {
            repository.loginUser(userIdentifier, role)
            isLoggedIn.value = true
            showAuthModal.value = false
        }
    }

    fun onUserRegistered(username: String, mobileNumber: String, email: String = "", role: String = "PLAYER") {
        viewModelScope.launch {
            repository.registerUser(username, mobileNumber, email, role)
            isLoggedIn.value = true
            showAuthModal.value = false
        }
    }

    fun loginGuestDemo() {
        viewModelScope.launch {
            repository.loginUser("p6692_user_1", "PLAYER")
            isLoggedIn.value = true
            showAuthModal.value = false
        }
    }

    fun logout() {
        isLoggedIn.value = false
        showAuthModal.value = true
        activeGameForPlay.value = null
        selectedGameForDetail.value = null
        showAdminDashboard.value = false
        showDepositModal.value = false
        currentNavScreen.value = NavScreen.HOME
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            database.gameDao().insertGames(CANONICAL_GAMES)
        }
    }

    fun onNavigate(screen: NavScreen) {
        currentNavScreen.value = screen
    }

    fun openGame(game: GameEntity) {
        selectedGameForDetail.value = null
        activeGameForPlay.value = game
    }

    fun exitGame() {
        activeGameForPlay.value = null
    }

    fun showGameDetail(game: GameEntity) {
        selectedGameForDetail.value = game
    }

    fun closeGameDetail() {
        selectedGameForDetail.value = null
    }

    fun openAiAdvisor(gameTitle: String = "Cyber Crash: Rocket 92") {
        aiAdvisorGameTitle.value = gameTitle
        showAiAdvisor.value = true
    }

    fun closeAiAdvisor() {
        showAiAdvisor.value = false
    }

    fun toggleFavorite(gameId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(gameId)
        }
    }

    fun toggleSound() {
        soundEnabled.value = !soundEnabled.value
    }

    // Real PKR Betting Flow
    suspend fun placePkrBet(gameId: String, gameTitle: String, betAmount: Double): Result<Double> {
        return repository.placePkrBet(gameId = gameId, gameTitle = gameTitle, betAmountPkr = betAmount)
    }

    fun creditPkrGameWin(gameId: String, gameTitle: String, winAmount: Double, multiplier: Double, betAmount: Double) {
        viewModelScope.launch {
            repository.creditPkrGameWin(
                gameId = gameId,
                gameTitle = gameTitle,
                winAmountPkr = winAmount,
                multiplier = multiplier,
                betAmountPkr = betAmount
            )
        }
    }

    fun recordPkrGameLoss(gameId: String, gameTitle: String, betAmount: Double) {
        viewModelScope.launch {
            repository.recordPkrGameLoss(
                gameId = gameId,
                gameTitle = gameTitle,
                betAmountPkr = betAmount
            )
        }
    }

    fun requestWithdrawal(
        amountPkr: Double,
        method: String,
        accountTitle: String,
        accountNumber: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.requestWithdrawal(
                amountPkr = amountPkr,
                method = method,
                accountTitle = accountTitle,
                accountNumber = accountNumber
            )
            if (res.isSuccess) {
                onResult(true, "Withdrawal request for PKR ${String.format("%,.0f", amountPkr)} submitted successfully!")
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Failed to process withdrawal request")
            }
        }
    }

    fun adminProcessWithdrawal(withdrawalId: Long, approve: Boolean, complete: Boolean, adminNote: String = "") {
        viewModelScope.launch {
            repository.adminProcessWithdrawal(withdrawalId, approve, complete, adminNote)
        }
    }

    fun claimDailyReward(reward: DailyRewardEntity) {
        viewModelScope.launch {
            repository.claimDailyReward(reward.day, reward.rewardCredits, reward.rewardPoints)
        }
    }

    fun claimMission(credits: Double, points: Int) {
        viewModelScope.launch {
            repository.claimDailyReward(99, credits, points)
        }
    }

    fun claimFaucet() {
        viewModelScope.launch {
            repository.claimHourlyFaucet(1000.0) // Adds PKR 1,000 faucet bonus
        }
    }

    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun setCustomAvatar(bitmap: Bitmap) {
        customAvatarBitmap.value = bitmap
    }

    fun switchRole(role: String) {
        viewModelScope.launch {
            repository.switchRole(role)
        }
    }

    fun adminToggleGameEnabled(gameId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.adminToggleGameEnabled(gameId, enabled)
        }
    }

    fun adminToggleGameFeatured(gameId: String, featured: Boolean) {
        viewModelScope.launch {
            repository.adminToggleGameFeatured(gameId, featured)
        }
    }

    fun adminGrantCredits(userId: String, amount: Double) {
        viewModelScope.launch {
            repository.adminGrantCredits(userId, amount)
        }
    }

    fun adminToggleBanUser(userId: String, isBanned: Boolean) {
        viewModelScope.launch {
            repository.adminToggleBanUser(userId, isBanned)
        }
    }

    fun submitDeposit(
        method: String,
        amountPkr: Double,
        orderId: String,
        gatewayTxnId: String = "",
        status: String = "PENDING"
    ) {
        viewModelScope.launch {
            repository.submitDeposit(method, amountPkr, orderId, gatewayTxnId, status)
        }
    }

    fun completePaymentVerification(
        orderId: String,
        method: String,
        amountPkr: Double,
        gatewayTxnId: String,
        isSuccess: Boolean,
        failureReason: String = ""
    ) {
        viewModelScope.launch {
            repository.completePaymentVerification(
                orderId,
                method,
                amountPkr,
                gatewayTxnId,
                isSuccess,
                failureReason
            )
        }
    }

    fun adminVerifyDeposit(depositId: Long, approve: Boolean, adminNote: String = "") {
        viewModelScope.launch {
            repository.adminVerifyDeposit(depositId, approve, adminNote)
        }
    }

    fun saveJazzCashConfig(config: JazzCashConfigEntity) {
        viewModelScope.launch {
            repository.saveJazzCashConfig(config)
        }
    }

    fun saveCryptoEzeConfig(config: CryptoEzeConfigEntity) {
        viewModelScope.launch {
            repository.saveCryptoEzeConfig(config)
        }
    }
}
