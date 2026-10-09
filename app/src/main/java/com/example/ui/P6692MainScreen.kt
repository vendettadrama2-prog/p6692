package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.games.*
import com.example.ui.screens.*
import com.example.ui.theme.DarkBg

@Composable
fun P6692MainApp(
    viewModel: P6692ViewModel = viewModel()
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val activeGames by viewModel.activeGames.collectAsStateWithLifecycle()
    val allGames by viewModel.allGames.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val gameHistory by viewModel.gameHistory.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val userWithdrawals by viewModel.userWithdrawals.collectAsStateWithLifecycle()
    val allWithdrawals by viewModel.allWithdrawals.collectAsStateWithLifecycle()
    val dailyRewards by viewModel.dailyRewards.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotifsCount.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val userDeposits by viewModel.userDeposits.collectAsStateWithLifecycle()
    val allDeposits by viewModel.allDeposits.collectAsStateWithLifecycle()
    val jazzCashConfig by viewModel.jazzCashConfig.collectAsStateWithLifecycle()

    val currentScreen by viewModel.currentNavScreen.collectAsStateWithLifecycle()
    val activeGame by viewModel.activeGameForPlay.collectAsStateWithLifecycle()
    val selectedDetailGame by viewModel.selectedGameForDetail.collectAsStateWithLifecycle()
    val showAiAdvisor by viewModel.showAiAdvisor.collectAsStateWithLifecycle()
    val aiAdvisorGameTitle by viewModel.aiAdvisorGameTitle.collectAsStateWithLifecycle()
    val customAvatar by viewModel.customAvatarBitmap.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val showAuthModal by viewModel.showAuthModal.collectAsStateWithLifecycle()
    val showAdminDashboard by viewModel.showAdminDashboard.collectAsStateWithLifecycle()
    val showNotificationsModal by viewModel.showNotificationsModal.collectAsStateWithLifecycle()
    val showDepositModal by viewModel.showDepositModal.collectAsStateWithLifecycle()

    // Ensure games catalogue is always instantly available, combining active & all games with canonical fallback
    val availableGames = remember(activeGames, allGames) {
        when {
            activeGames.isNotEmpty() -> activeGames
            allGames.isNotEmpty() -> allGames
            else -> CANONICAL_GAMES
        }
    }

    // Handle system back navigation
    BackHandler(
        enabled = activeGame != null || selectedDetailGame != null || showAiAdvisor ||
                showAdminDashboard || showNotificationsModal || showAuthModal || showDepositModal || currentScreen != NavScreen.HOME
    ) {
        when {
            activeGame != null -> viewModel.exitGame()
            selectedDetailGame != null -> viewModel.closeGameDetail()
            showAiAdvisor -> viewModel.closeAiAdvisor()
            showAdminDashboard -> viewModel.showAdminDashboard.value = false
            showNotificationsModal -> viewModel.showNotificationsModal.value = false
            showAuthModal -> viewModel.showAuthModal.value = false
            showDepositModal -> viewModel.showDepositModal.value = false
            currentScreen != NavScreen.HOME -> viewModel.onNavigate(NavScreen.HOME)
        }
    }

    // 1. Fullscreen Playable Real PKR Game Mode
    if (activeGame != null) {
        val currentGame = activeGame!!
        when (currentGame.id) {
            "cyber_crash" -> {
                CyberCrashGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            "neon_spin", "neon_slots", "slots_777" -> {
                NeonSlotsGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            "mines", "mines_game" -> {
                MinesGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            "plinko", "neon_plinko", "plinko_game" -> {
                PlinkoGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            "cyber_mines" -> {
                CyberMinesGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            "jelly", "jelly_game" -> {
                JellyGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            "aviator", "aviator_game", "rocket", "rocket_aviator", "crash" -> {
                AviatorGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            "bubble_shooter", "bubble" -> {
                BubbleShooterGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            "candy_match", "candy" -> {
                CandyMatchGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            "fruit_merge", "fruit" -> {
                FruitMergeGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            "money_coming", "money_coming_game", "spinning_wheel", "spin", "spain", "spin_wheel", "quantum_roulette" -> {
                MoneyComingGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            "fortune_gems", "fortune_gems_2", "fortune_gems_game" -> {
                FortuneGemsGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            "car_racing", "racing" -> {
                CarRacingGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            "ludo_game", "ludo" -> {
                LudoGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            "subway_runner", "runner" -> {
                SubwayRunnerGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            "piano_tiles", "piano" -> {
                PianoTilesGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
            else -> {
                // Default fallback to Cyber Crash for other arcade games
                CyberCrashGame(
                    pkrBalance = user?.pkrBalance ?: 25000.0,
                    soundEnabled = soundEnabled,
                    onPlaceBet = { bet -> viewModel.placePkrBet(currentGame.id, currentGame.title, bet) },
                    onCashoutWin = { win, mult, bet -> viewModel.creditPkrGameWin(currentGame.id, currentGame.title, win, mult, bet) },
                    onRoundLost = { bet -> viewModel.recordPkrGameLoss(currentGame.id, currentGame.title, bet) },
                    onOpenDeposit = { viewModel.showDepositModal.value = true },
                    onBack = { viewModel.exitGame() }
                )
            }
        }
        return
    }

    // 2. Admin Dashboard Screen (Strict Role-Based Access Control)
    if (showAdminDashboard) {
        if (user?.role?.uppercase() == "ADMIN") {
            AdminDashboardScreen(
                users = allUsers,
                games = allGames,
                deposits = allDeposits,
                withdrawals = allWithdrawals,
                transactions = allTransactions,
                jazzCashConfig = jazzCashConfig,
                auditLogs = auditLogs,
                onToggleGameEnabled = { id, en -> viewModel.adminToggleGameEnabled(id, en) },
                onToggleGameFeatured = { id, feat -> viewModel.adminToggleGameFeatured(id, feat) },
                onGrantCredits = { id, amt -> viewModel.adminGrantCredits(id, amt) },
                onToggleBanUser = { id, ban -> viewModel.adminToggleBanUser(id, ban) },
                onVerifyDeposit = { id, app, note -> viewModel.adminVerifyDeposit(id, app, note) },
                onProcessWithdrawal = { id, app, comp, note -> viewModel.adminProcessWithdrawal(id, app, comp, note) },
                onSaveJazzCashConfig = { cfg -> viewModel.saveJazzCashConfig(cfg) },
                onBack = { viewModel.showAdminDashboard.value = false }
            )
        } else {
            // Non-admin attempting direct navigation - auto-close and deny access
            viewModel.showAdminDashboard.value = false
        }
        return
    }

    // 3. Notifications Screen
    if (showNotificationsModal) {
        NotificationsScreen(
            notifications = notifications,
            onMarkAsRead = { viewModel.markNotificationAsRead(it) },
            onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() },
            onBack = { viewModel.showNotificationsModal.value = false }
        )
        return
    }

    // 4. Auth Modal
    if (showAuthModal) {
        AuthScreen(
            onLoginSuccess = { _, role ->
                viewModel.switchRole(role)
                viewModel.showAuthModal.value = false
            },
            onClose = { viewModel.showAuthModal.value = false }
        )
        return
    }

    // 5. Real PKR Deposit Screen (Modern Daraz-style checkout separate from lobby)
    if (showDepositModal) {
        DepositScreen(
            pkrBalance = user?.pkrBalance ?: 25000.0,
            jazzCashConfig = jazzCashConfig,
            userDeposits = userDeposits,
            onCompletePayment = { orderId, method, amt, gatewayTxnId, isSuccess, failureReason ->
                viewModel.completePaymentVerification(orderId, method, amt, gatewayTxnId, isSuccess, failureReason)
            },
            onClose = { viewModel.showDepositModal.value = false }
        )
        return
    }

    // 6. Standard Main Platform Scaffold (Header, Content, Bottom Navigation)
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        topBar = {
            P6692TopHeader(
                pkrBalance = user?.pkrBalance ?: 25000.0,
                vipLevel = user?.vipLevel ?: 3,
                unreadNotifications = unreadCount,
                soundEnabled = soundEnabled,
                onBalanceClick = { viewModel.onNavigate(NavScreen.WALLET) },
                onNotificationsClick = { viewModel.showNotificationsModal.value = true },
                onSoundToggle = { viewModel.toggleSound() },
                onLogoClick = { viewModel.onNavigate(NavScreen.HOME) }
            )
        },
        bottomBar = {
            P6692BottomNav(
                currentScreen = currentScreen,
                onNavigate = { viewModel.onNavigate(it) }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
        ) {
            // Main content depending on active tab
            when (currentScreen) {
                NavScreen.HOME -> {
                    HomeScreen(
                        user = user,
                        games = availableGames,
                        favoriteIds = favoriteIds,
                        onGameClick = { viewModel.showGameDetail(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onPlayGame = { viewModel.openGame(it) },
                        onClaimDaily = { viewModel.onNavigate(NavScreen.PROMOTIONS) },
                        onOpenAiAdvisor = { viewModel.openAiAdvisor(it) },
                        onViewAllGames = { viewModel.onNavigate(NavScreen.LOBBY) },
                        onOpenWallet = { viewModel.onNavigate(NavScreen.WALLET) },
                        onOpenDeposit = { viewModel.showDepositModal.value = true }
                    )
                }
                NavScreen.LOBBY -> {
                    LobbyScreen(
                        games = availableGames,
                        favoriteIds = favoriteIds,
                        onGameClick = { viewModel.showGameDetail(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onPlayGame = { viewModel.openGame(it) }
                    )
                }
                NavScreen.WALLET -> {
                    WalletScreen(
                        user = user,
                        transactions = transactions,
                        withdrawals = userWithdrawals,
                        onClaimFaucet = { viewModel.claimFaucet() },
                        onOpenDeposit = { viewModel.showDepositModal.value = true },
                        onRequestWithdrawal = { amt, method, title, num, callback ->
                            viewModel.requestWithdrawal(amt, method, title, num, callback)
                        }
                    )
                }
                NavScreen.PROMOTIONS -> {
                    PromotionsScreen(
                        user = user,
                        dailyRewards = dailyRewards,
                        onClaimDayReward = { viewModel.claimDailyReward(it) },
                        onClaimMission = { credits, points ->
                            viewModel.claimMission(credits, points)
                        }
                    )
                }
                NavScreen.PROFILE -> {
                    ProfileScreen(
                        user = user,
                        gameHistory = gameHistory,
                        customAvatarBitmap = customAvatar,
                        onOpenAiStudio = { viewModel.openAiAdvisor("Platform") },
                        onOpenAdminDashboard = { viewModel.showAdminDashboard.value = true },
                        onToggleRole = { role -> viewModel.switchRole(role) },
                        onLogout = { viewModel.showAuthModal.value = true }
                    )
                }
            }

            // Game Detail Sheet Modal
            if (selectedDetailGame != null) {
                GameDetailSheet(
                    game = selectedDetailGame!!,
                    isFavorite = favoriteIds.contains(selectedDetailGame!!.id),
                    onDismiss = { viewModel.closeGameDetail() },
                    onToggleFavorite = { viewModel.toggleFavorite(selectedDetailGame!!.id) },
                    onPlayGame = {
                        val g = selectedDetailGame!!
                        viewModel.closeGameDetail()
                        viewModel.openGame(g)
                    },
                    onAskAiStrategy = {
                        val title = selectedDetailGame!!.title
                        viewModel.closeGameDetail()
                        viewModel.openAiAdvisor(title)
                    }
                )
            }

            // Gemini AI Strategist & Studio Sheet
            if (showAiAdvisor) {
                GeminiAiAdvisorSheet(
                    initialGameTitle = aiAdvisorGameTitle,
                    onDismiss = { viewModel.closeAiAdvisor() },
                    onAvatarGenerated = { bitmap -> viewModel.setCustomAvatar(bitmap) }
                )
            }
        }
    }
}
