package es.uc3m.android.chillmates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.chillmates.viewmodel.AuthState
import es.uc3m.android.chillmates.viewmodel.AuthViewModel
import es.uc3m.android.chillmates.viewmodel.FlatSetupMode

@Composable
fun LoginPage(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Redirect to home if already authenticated
    LaunchedEffect(uiState.authState) {
        if (uiState.authState is AuthState.Authenticated) {
            navController.navigate(NavGraph.Home.route) {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // If user is logged in but has no flat, show setup
    if (uiState.authState is AuthState.NeedsFlatSetup) {
        FlatSetupPage(
            authViewModel = authViewModel,
            onComplete = {
                navController.navigate(NavGraph.Home.route) {
                    popUpTo("login") { inclusive = true }
                }
            }
        )
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (uiState.isLoginMode) stringResource(R.string.login_welcome_title) else stringResource(R.string.signup_welcome_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!uiState.isLoginMode) {
                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = { authViewModel.onDisplayNameChange(it) },
                    label = { Text(stringResource(R.string.login_label_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { authViewModel.onEmailChange(it) },
                label = { Text(stringResource(R.string.login_label_email)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = { authViewModel.onPasswordChange(it) },
                label = { Text(stringResource(R.string.login_label_password)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (uiState.isLoginMode) ImeAction.Done else ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = {
                        focusManager.clearFocus()
                        if (uiState.isLoginMode) authViewModel.login()
                    }
                )
            )

            if (!uiState.isLoginMode) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = { authViewModel.onConfirmPasswordChange(it) },
                    label = { Text(stringResource(R.string.login_label_confirm_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        authViewModel.signUp()
                    })
                )
            }

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = uiState.errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { if (uiState.isLoginMode) authViewModel.login() else authViewModel.signUp() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isLoading
            ) {
                Text(
                    text = if (uiState.isLoginMode) stringResource(R.string.login_button) else stringResource(R.string.signup_button),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { authViewModel.onToggleMode() }) {
                Text(text = if (uiState.isLoginMode) stringResource(R.string.login_footer_signup) else stringResource(R.string.signup_footer_login))
            }
        }
    }
}

@Composable
fun FlatSetupPage(
    authViewModel: AuthViewModel,
    onComplete: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.authState) {
        if (uiState.authState is AuthState.Authenticated) {
            onComplete()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.flat_setup_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.flat_setup_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = uiState.flatSetupMode == FlatSetupMode.CREATE,
                    onClick = { authViewModel.setFlatSetupMode(FlatSetupMode.CREATE) },
                    label = { Text(stringResource(R.string.flat_setup_create)) },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = uiState.flatSetupMode == FlatSetupMode.JOIN,
                    onClick = { authViewModel.setFlatSetupMode(FlatSetupMode.JOIN) },
                    label = { Text(stringResource(R.string.flat_setup_join)) },
                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState.flatSetupMode) {
                FlatSetupMode.CREATE -> {
                    OutlinedTextField(
                        value = uiState.flatName,
                        onValueChange = { authViewModel.onFlatNameChange(it) },
                        label = { Text(stringResource(R.string.flat_setup_flat_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            authViewModel.completeFlatSetup()
                        })
                    )
                }
                FlatSetupMode.JOIN -> {
                    OutlinedTextField(
                        value = uiState.inviteCode,
                        onValueChange = { authViewModel.onInviteCodeChange(it) },
                        label = { Text(stringResource(R.string.flat_setup_invite_code_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            authViewModel.completeFlatSetup()
                        })
                    )
                }
                else -> {}
            }

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = uiState.errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { authViewModel.completeFlatSetup() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isLoading && uiState.flatSetupMode != FlatSetupMode.NONE
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(text = stringResource(R.string.flat_setup_continue), style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { authViewModel.logout() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Already have an account? Log in")
            }
        }
    }
}