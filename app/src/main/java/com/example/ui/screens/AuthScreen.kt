package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.repository.CineCutRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    repository: CineCutRepository,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }

    // Form inputs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    var showApiKeyDialog by remember { mutableStateOf(false) }
    val firebasePrefs = remember { context.getSharedPreferences("cinecut_firebase_prefs", android.content.Context.MODE_PRIVATE) }
    var enteredApiKey by remember { mutableStateOf(firebasePrefs.getString("custom_firebase_api_key", "") ?: "") }
    var enteredWebClientId by remember { mutableStateOf(firebasePrefs.getString("custom_google_web_client_id", "") ?: "") }
    var enteredProjectId by remember { mutableStateOf(firebasePrefs.getString("custom_firebase_project_id", "") ?: "") }
    var keyDialogError by remember { mutableStateOf<String?>(null) }
    var hasLiveCloud by remember { mutableStateOf(repository.hasConfiguredLiveApiKey()) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CineBackground)
            .testTag("auth_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // CineCut Brand Logo & Shield
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(CinePrimary, CineSecondary, CineTertiary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MovieFilter,
                    contentDescription = "CineCut Logo",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CineCut",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Text(
                text = "Professional Filmmaking & Multi-Track Studio",
                fontSize = 12.sp,
                color = CineTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Main Auth Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CineSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tab Selector: Login vs Register
                    if (authMode != AuthMode.FORGOT_PASSWORD) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CineBackground)
                                .padding(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    authMode = AuthMode.LOGIN
                                    errorMessage = null
                                    infoMessage = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (authMode == AuthMode.LOGIN) CinePrimary else Color.Transparent,
                                    contentColor = if (authMode == AuthMode.LOGIN) Color.White else CineTextSecondary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tab_login")
                            ) {
                                Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    authMode = AuthMode.REGISTER
                                    errorMessage = null
                                    infoMessage = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (authMode == AuthMode.REGISTER) CinePrimary else Color.Transparent,
                                    contentColor = if (authMode == AuthMode.REGISTER) Color.White else CineTextSecondary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tab_register")
                            ) {
                                Text("Register", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    } else {
                        // Forgot Password Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { authMode = AuthMode.LOGIN }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back to login", tint = Color.White)
                            }
                            Text(
                                "Reset Password",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            "Enter your verified account email to receive a password recovery link.",
                            fontSize = 12.sp,
                            color = CineTextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Alerts
                    AnimatedVisibility(visible = errorMessage != null, enter = fadeIn(), exit = fadeOut()) {
                        errorMessage?.let { error ->
                            Surface(
                                color = CineError.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CineError),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CineError)
                                    Text(error, fontSize = 12.sp, color = CineError, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = infoMessage != null, enter = fadeIn(), exit = fadeOut()) {
                        infoMessage?.let { info ->
                            Surface(
                                color = CineSuccess.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CineSuccess),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = CineSuccess)
                                    Text(info, fontSize = 12.sp, color = CineSuccess, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Form Fields
                    if (authMode == AuthMode.REGISTER) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Full Name / Creator Name") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = CineTertiary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinePrimary,
                                unfocusedBorderColor = CineTimelineRuler,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' } },
                            label = { Text("Username (@handle)") },
                            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = CineTertiary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinePrimary,
                                unfocusedBorderColor = CineTimelineRuler,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_username_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CineTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CinePrimary,
                            unfocusedBorderColor = CineTimelineRuler,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = if (authMode == AuthMode.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )

                    if (authMode != AuthMode.FORGOT_PASSWORD) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CineTertiary) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = CineTextSecondary
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinePrimary,
                                unfocusedBorderColor = CineTimelineRuler,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (authMode == AuthMode.LOGIN) ImeAction.Done else ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                onDone = { focusManager.clearFocus() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input")
                        )
                    }

                    if (authMode == AuthMode.REGISTER) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Confirm Password Field
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = CineTertiary) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinePrimary,
                                unfocusedBorderColor = CineTimelineRuler,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_confirm_password_input")
                        )
                    }

                    if (authMode == AuthMode.LOGIN) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    authMode = AuthMode.FORGOT_PASSWORD
                                    errorMessage = null
                                    infoMessage = null
                                },
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text("Forgot Password?", fontSize = 12.sp, color = CineTertiary)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Primary Submit Button
                    Button(
                        onClick = {
                            errorMessage = null
                            infoMessage = null
                            focusManager.clearFocus()

                            when (authMode) {
                                AuthMode.LOGIN -> {
                                    if (email.isBlank() || password.isBlank()) {
                                        errorMessage = "Please enter both email and password."
                                        return@Button
                                    }
                                    isLoading = true
                                    coroutineScope.launch {
                                        val res = repository.signInWithEmail(email, password)
                                        isLoading = false
                                        res.fold(
                                            onSuccess = { profile ->
                                                onAuthSuccess()
                                            },
                                            onFailure = { ex ->
                                                errorMessage = ex.localizedMessage ?: "Sign-in failed. Please verify credentials."
                                            }
                                        )
                                    }
                                }
                                AuthMode.REGISTER -> {
                                    if (displayName.isBlank() || email.isBlank() || password.isBlank()) {
                                        errorMessage = "All fields are required."
                                        return@Button
                                    }
                                    if (password != confirmPassword) {
                                        errorMessage = "Passwords do not match."
                                        return@Button
                                    }
                                    if (password.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@Button
                                    }
                                    isLoading = true
                                    coroutineScope.launch {
                                        val res = repository.registerWithEmail(
                                            displayName = displayName,
                                            username = username.ifBlank { email.substringBefore("@") },
                                            email = email,
                                            pass = password
                                        )
                                        isLoading = false
                                        res.fold(
                                            onSuccess = {
                                                infoMessage = "Account registered successfully! Verification email dispatched."
                                                onAuthSuccess()
                                            },
                                            onFailure = { ex ->
                                                errorMessage = ex.localizedMessage ?: "Registration failed."
                                            }
                                        )
                                    }
                                }
                                AuthMode.FORGOT_PASSWORD -> {
                                    if (email.isBlank()) {
                                        errorMessage = "Please enter your account email address."
                                        return@Button
                                    }
                                    isLoading = true
                                    coroutineScope.launch {
                                        val res = repository.sendPasswordReset(email)
                                        isLoading = false
                                        res.fold(
                                            onSuccess = {
                                                infoMessage = "Password reset instructions sent to $email."
                                            },
                                            onFailure = { ex ->
                                                errorMessage = ex.localizedMessage ?: "Could not dispatch reset email."
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = when (authMode) {
                                    AuthMode.LOGIN -> "Sign In to CineCut"
                                    AuthMode.REGISTER -> "Create Creator Account"
                                    AuthMode.FORGOT_PASSWORD -> "Send Recovery Link"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Divider: Or Continue With
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = CineTimelineRuler)
                        Text(
                            text = "  OR CONTINUE WITH  ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextSecondary
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = CineTimelineRuler)
                    }

                    // Real Firebase Google OAuth Button
                    OutlinedButton(
                        onClick = {
                            errorMessage = null
                            infoMessage = null
                            focusManager.clearFocus()
                            val activity = context as? Activity
                            if (activity == null) {
                                errorMessage = "Google Sign-In requires an active Activity context."
                                return@OutlinedButton
                            }
                            isLoading = true
                            coroutineScope.launch {
                                val res = repository.signInWithRealGoogleOAuth(activity)
                                isLoading = false
                                res.fold(
                                    onSuccess = { profile ->
                                        infoMessage = "Welcome, ${profile.displayName}! Signed in with Google."
                                        onAuthSuccess()
                                    },
                                    onFailure = { ex ->
                                        val msg = ex.message.orEmpty()
                                        if (ex is com.google.firebase.auth.FirebaseAuthException &&
                                            (ex.errorCode == "ERROR_WEB_CONTEXT_CANCELED" || msg.contains("canceled", ignoreCase = true))
                                        ) {
                                            // User dismissed web OAuth prompt
                                            return@launch
                                        }
                                        if (ex is androidx.credentials.exceptions.GetCredentialCancellationException ||
                                            msg.contains("canceled", ignoreCase = true) ||
                                            msg.contains("cancellation", ignoreCase = true)
                                        ) {
                                            // User dismissed Credential Manager prompt
                                            return@launch
                                        }
                                        if (msg.contains("api key", ignoreCase = true) || msg.contains("valid key", ignoreCase = true)) {
                                            errorMessage = "Firebase API key required for live cloud OAuth. Set FIREBASE_API_KEY in the AI Studio Secrets panel."
                                        } else {
                                            errorMessage = ex.localizedMessage ?: "Google Sign-In failed."
                                        }
                                    }
                                )
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_oauth_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CineTertiary.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CineSurfaceHighlight,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            GoogleLogoIcon(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cloud & Firebase Configuration Status Card
                    Surface(
                        color = if (hasLiveCloud) CineSuccess.copy(alpha = 0.12f) else CineSurfaceHighlight,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (hasLiveCloud) CineSuccess.copy(alpha = 0.4f) else CineTertiary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("firebase_cloud_status_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    if (hasLiveCloud) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (hasLiveCloud) CineSuccess else CineTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (hasLiveCloud) "Live Firebase Cloud Connected" else "CineCut Studio Mode Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        if (hasLiveCloud)
                                            "Connected to cloud authentication and real-time backend sync."
                                        else
                                            "All video editing, AI director & timeline features unlocked. To connect a live Firebase project, paste your FIREBASE_API_KEY below or in AI Studio Secrets.",
                                        fontSize = 10.sp,
                                        color = CineTextSecondary,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showApiKeyDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .testTag("enter_api_key_button"),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CineTertiary.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = CineSurface,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.VpnKey,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = CineTertiary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (hasLiveCloud) "Update Firebase API Key" else "🔑 Enter Firebase API Key",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Initial Admin Security Notice
                    Surface(
                        color = CineSurfaceHighlight,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CineTertiary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(16.dp))
                                Text(
                                    "Enterprise Role-Based Security",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                "No auto-login or default sessions exist. Privileged roles require backend Custom Claims verification on Firebase Authentication. Authorized Administrator: robinisking3@gmail.com (Email alone never grants privileges).",
                                fontSize = 10.sp,
                                color = CineTextSecondary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Protected by Firebase App Check & Multi-Layer Authorization",
                fontSize = 10.sp,
                color = CineTextSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        if (showApiKeyDialog) {
            AlertDialog(
                onDismissRequest = { showApiKeyDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enter Firebase Web API Key", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Paste your Web API Key from Firebase Console (Project settings > General > Web API Key). It starts with \"AIzaSy...\".",
                            fontSize = 12.sp,
                            color = CineTextSecondary
                        )
                        OutlinedTextField(
                            value = enteredApiKey,
                            onValueChange = {
                                enteredApiKey = it
                                keyDialogError = null
                            },
                            label = { Text("FIREBASE_API_KEY", fontSize = 12.sp) },
                            placeholder = { Text("AIzaSy...", fontSize = 12.sp) },
                            singleLine = true,
                            isError = keyDialogError != null,
                            supportingText = keyDialogError?.let { { Text(it, color = CineError, fontSize = 11.sp) } },
                            modifier = Modifier.fillMaxWidth().testTag("api_key_dialog_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CineTertiary,
                                unfocusedBorderColor = CineTertiary.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = enteredWebClientId,
                            onValueChange = { enteredWebClientId = it },
                            label = { Text("Google Web Client ID (Optional for OAuth)", fontSize = 12.sp) },
                            placeholder = { Text("*.apps.googleusercontent.com", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("web_client_id_dialog_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CineTertiary,
                                unfocusedBorderColor = CineTertiary.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = enteredProjectId,
                            onValueChange = { enteredProjectId = it },
                            label = { Text("Project ID (Optional)", fontSize = 12.sp) },
                            placeholder = { Text("cinecut-prod-app", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("project_id_dialog_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CineTertiary,
                                unfocusedBorderColor = CineTertiary.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = enteredApiKey.trim()
                            if (!trimmed.startsWith("AIza") || trimmed.length < 30) {
                                keyDialogError = "Key must start with 'AIza' and be at least 30 characters long."
                                return@Button
                            }
                            val success = repository.updateCustomFirebaseApiKey(
                                apiKey = trimmed,
                                projectId = enteredProjectId.trim().takeIf { it.isNotBlank() },
                                webClientId = enteredWebClientId.trim().takeIf { it.isNotBlank() }
                            )
                            if (success) {
                                hasLiveCloud = true
                                showApiKeyDialog = false
                                infoMessage = "Firebase credentials successfully connected! Live cloud features active."
                                errorMessage = null
                            } else {
                                keyDialogError = "Failed to initialize Firebase with this key. Check key format."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                        modifier = Modifier.testTag("save_api_key_button")
                    ) {
                        Text("Save & Connect", fontSize = 12.sp, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showApiKeyDialog = false }) {
                        Text("Cancel", fontSize = 12.sp, color = CineTextSecondary)
                    }
                },
                containerColor = CineSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/**
 * Pixel-perfect Google G logo rendered directly with Canvas using official brand colors.
 */
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.18f

        val red = Color(0xFFEA4335)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)
        val blue = Color(0xFF4285F4)

        // Red arc (Top)
        drawArc(
            color = red,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        // Yellow arc (Left)
        drawArc(
            color = yellow,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        // Green arc (Bottom)
        drawArc(
            color = green,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        // Blue arc (Right)
        drawArc(
            color = blue,
            startAngle = -45f,
            sweepAngle = 45f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        // Blue horizontal crossbar
        drawLine(
            color = blue,
            start = androidx.compose.ui.geometry.Offset(w * 0.45f, h * 0.5f),
            end = androidx.compose.ui.geometry.Offset(w, h * 0.5f),
            strokeWidth = stroke
        )
    }
}
