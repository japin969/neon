package app.neon.com.ui.security

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.neon.com.ui.chat.neontChatPalette

@Composable
fun AppLockGate(
    modifier: Modifier = Modifier,
    relockAfterMillis: Long = 0L,
    onSignedOut: (() -> Unit)? = null,
    viewModel: AppLockViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    val palette = neontChatPalette()
    var showForgotDialog by remember { mutableStateOf(false) }
    val isUnlocked = state.phase == AppLockPhase.Unlocked

    DisposableEffect(activity, relockAfterMillis) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (activity?.isChangingConfigurations != true) viewModel.onAppBackgrounded()
                }
                Lifecycle.Event.ON_START -> viewModel.onAppForegrounded(relockAfterMillis)
                else -> Unit
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().then(if (isUnlocked) Modifier else Modifier.clearAndSetSemantics { })) {
            content()
        }
        if (!isUnlocked) {
            BackHandler { activity?.moveTaskToBack(true) }
            if (state.phase == AppLockPhase.Loading) {
                Box(modifier = Modifier.fillMaxSize().background(palette.listBackground).pointerInput(Unit) { })
            } else {
                PinScreenContent(
                    title = "Masukkan PIN",
                    subtitle = "Neont terkunci",
                    pad = state.pad,
                    onDigit = viewModel::onDigit,
                    onBackspace = viewModel::onBackspace,
                    modifier = Modifier.pointerInput(Unit) { },
                    footer = {
                        TextButton(onClick = { showForgotDialog = true }) {
                            Text("Lupa PIN?", color = palette.primary)
                        }
                    }
                )
            }
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            containerColor = palette.surface,
            title = { Text("Lupa PIN?") },
            text = { Text("PIN tidak bisa dipulihkan. Anda akan keluar dari akun dan PIN dihapus, lalu perlu masuk lagi.") },
            confirmButton = {
                TextButton(onClick = {
                    showForgotDialog = false
                    viewModel.forgotPin {
                        if (onSignedOut != null) onSignedOut() else activity?.recreate()
                    }
                }) { Text("Keluar dari akun", color = palette.destructive) }
            },
            dismissButton = { TextButton(onClick = { showForgotDialog = false }) { Text("Batal") } }
        )
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
