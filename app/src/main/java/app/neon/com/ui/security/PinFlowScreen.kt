package app.neon.com.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.neon.com.ui.chat.neontChatPalette

@Composable
fun PinFlowScreen(
    flowType: PinFlowType,
    onFinished: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PinFlowViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val palette = neontChatPalette()
    val step = state.step

    LaunchedEffect(flowType) { viewModel.start(flowType) }
    LaunchedEffect(state.isFinished) { if (state.isFinished) onFinished() }

    Box(modifier = modifier.fillMaxSize().background(palette.listBackground)) {
        if (step != null) {
            PinScreenContent(
                title = titleFor(step),
                subtitle = subtitleFor(flowType, step),
                pad = state.pad,
                onDigit = viewModel::onDigit,
                onBackspace = viewModel::onBackspace
            )
        }
        IconButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(4.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = palette.textPrimary)
        }
    }
}

private fun titleFor(step: PinStep): String = when (step) {
    PinStep.VerifyCurrent -> "Masukkan PIN saat ini"
    PinStep.Create -> "Buat PIN baru"
    PinStep.Confirm -> "Konfirmasi PIN"
}

private fun subtitleFor(flowType: PinFlowType, step: PinStep): String = when (step) {
    PinStep.VerifyCurrent -> when (flowType) {
        PinFlowType.Disable -> "Untuk menonaktifkan kunci aplikasi"
        else -> "Untuk mengubah PIN"
    }
    PinStep.Create -> "Masukkan 4 digit angka"
    PinStep.Confirm -> "Masukkan PIN yang sama sekali lagi"
}
