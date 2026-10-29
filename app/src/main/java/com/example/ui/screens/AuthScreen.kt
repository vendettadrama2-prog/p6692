package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LogoSize
import com.example.ui.components.P6692Logo
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    onLoginSuccess: (String, String) -> Unit,
    onRegisterSuccess: (username: String, mobileNumber: String, password: String, email: String) -> Unit = { _, _, _, _ -> },
    onGuestDemo: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    isStartupScreen: Boolean = false
) {
    var mode by remember { mutableStateOf(if (isStartupScreen) "REGISTER" else "LOGIN") } // LOGIN, REGISTER, FORGOT, OTP
    var usernameOrEmail by remember { mutableStateOf("player@p6692.com") }
    var password by remember { mutableStateOf("cyber123") }
    var regUsername by remember { mutableStateOf("") }
    var regMobile by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successNotice by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = DarkBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top Close (Only when opened as modal from inside app, not startup screen)
            if (!isStartupScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { onClose?.invoke() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Logo
            P6692Logo(size = LogoSize.LARGE, showSubtext = true)

            if (isStartupScreen) {
                Text(
                    text = "🇵🇰 Pakistan's Real PKR Gaming & Instant JazzCash Payments Portal",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Auth Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardBg,
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Mode Tabs
                    if (mode == "LOGIN" || mode == "REGISTER") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("LOGIN" to "Sign In", "REGISTER" to "Register").forEach { (m, title) ->
                                val isSel = mode == m
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            mode = m
                                            errorMessage = null
                                        },
                                    color = if (isSel) PrimaryPurple else SecondaryBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BrightPurple else CardBorder)
                                ) {
                                    Text(
                                        text = title,
                                        color = if (isSel) Color.White else TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = CoralRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = CoralRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    if (successNotice != null) {
                        Surface(
                            color = NeonGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen)
                        ) {
                            Text(
                                text = successNotice!!,
                                color = NeonGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    when (mode) {
                        "LOGIN" -> {
                            OutlinedTextField(
                                value = usernameOrEmail,
                                onValueChange = { usernameOrEmail = it },
                                label = { Text("Email or Username") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = AccentCyan) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = BrightPurple) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Forgot password?",
                                    color = AccentCyan,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { mode = "FORGOT" }
                                )
                            }

                            Button(
                                onClick = {
                                    if (usernameOrEmail.isBlank() || password.isBlank()) {
                                        errorMessage = "Please enter your username and password."
                                    } else {
                                        val role = if (usernameOrEmail.contains("admin", ignoreCase = true)) "ADMIN" else "PLAYER"
                                        onLoginSuccess(usernameOrEmail, role)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                            ) {
                                Text("SIGN IN TO P6692", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        "REGISTER" -> {
                            OutlinedTextField(
                                value = regUsername,
                                onValueChange = { regUsername = it; errorMessage = null },
                                label = { Text("Choose Username") },
                                placeholder = { Text("e.g. CyberGamer_92") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = AccentCyan) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = regMobile,
                                onValueChange = { regMobile = it; errorMessage = null },
                                label = { Text("Mobile Number (JazzCash / Easypaisa)") },
                                placeholder = { Text("03001234567") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = NeonGreen) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = { regEmail = it; errorMessage = null },
                                label = { Text("Email Address (Optional)") },
                                placeholder = { Text("player@example.com") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it; errorMessage = null },
                                label = { Text("Choose Password") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = BrightPurple) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                singleLine = true
                            )

                            Surface(
                                color = NeonGreen.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, NeonGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💡 New accounts start with PKR 0.00 Real Wallet and PKR 5,000 DEMO practice credits.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    if (regUsername.trim().length < 3) {
                                        errorMessage = "Please enter a username with at least 3 characters."
                                    } else if (regPassword.length < 4) {
                                        errorMessage = "Please enter a password with at least 4 characters."
                                    } else if (regMobile.trim().length < 10) {
                                        errorMessage = "Please enter a valid JazzCash / Easypaisa mobile number (03XXXXXXXXX)."
                                    } else {
                                        onRegisterSuccess(regUsername.trim(), regMobile.trim(), regPassword, regEmail.trim())
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrightPurple)
                            ) {
                                Text("REGISTER ACCOUNT & ENTER", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        "FORGOT" -> {
                            Text("Reset Password", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Enter your email to receive a recovery OTP code:", color = TextSecondary, fontSize = 12.sp)

                            OutlinedTextField(
                                value = usernameOrEmail,
                                onValueChange = { usernameOrEmail = it },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Button(
                                onClick = {
                                    mode = "OTP"
                                    successNotice = "OTP Code sent to $usernameOrEmail. Enter 669200 to verify."
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                            ) {
                                Text("SEND OTP CODE")
                            }

                            TextButton(onClick = { mode = "LOGIN" }) {
                                Text("Back to Login", color = TextSecondary)
                            }
                        }

                        "OTP" -> {
                            Text("Verification OTP Code", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Enter the 6-digit code (use demo code: 669200):", color = TextSecondary, fontSize = 12.sp)

                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { otpCode = it },
                                label = { Text("6-Digit Code") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Button(
                                onClick = {
                                    if (otpCode == "669200" || otpCode.length >= 4) {
                                        if (regUsername.isNotBlank()) {
                                            onRegisterSuccess(regUsername.trim(), regMobile.trim(), regPassword, regEmail.trim())
                                        } else {
                                            onLoginSuccess(usernameOrEmail, "PLAYER")
                                        }
                                    } else {
                                        errorMessage = "Invalid code. Use demo code: 669200"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                            ) {
                                Text("VERIFY & ENTER P6692", color = DarkBg, fontWeight = FontWeight.Bold)
                            }

                            TextButton(onClick = { mode = "LOGIN" }) {
                                Text("Back to Login", color = TextSecondary)
                            }
                        }
                    }

                    // Demo Quick Switcher & Guest Practice Mode
                    HorizontalDivider(color = CardBorder)
                    Text("Instant Access & Practice:", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Button(
                        onClick = onGuestDemo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldYellow.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldYellow)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = GoldYellow, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TRY DEMO PRACTICE (PKR 5,000 DEMO)", color = GoldYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onLoginSuccess("CyberKnight_92", "PLAYER") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple)
                        ) {
                            Text("Player Demo", color = Color.White, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { onLoginSuccess("Admin_P6692", "ADMIN") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan)
                        ) {
                            Text("Admin Console", color = AccentCyan, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
