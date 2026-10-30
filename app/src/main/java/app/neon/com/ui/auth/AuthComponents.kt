package app.neon.com.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

internal val AuthBackground = Color(0xFF1C1C1E)
internal val AuthSurface = Color(0xFF2C2C2E)
internal val AuthPrimary = Color(0xFF0A84FF)
internal val AuthPrimaryLight = Color(0xFF409CFF)
internal val AuthTextSecondary = Color(0xFF8E8E93)
internal val AuthBorder = Color(0xFF3A3A3C)
internal val AuthError = Color(0xFFFF453A)
internal val AuthSuccess = Color(0xFF30D158)
internal val AuthCardShape = RoundedCornerShape(16.dp)
internal val AuthInputShape = RoundedCornerShape(12.dp)
internal val AuthButtonShape = RoundedCornerShape(12.dp)

@Composable
internal fun AuthScreenContainer(content: @Composable ColumnScope.() -> Unit) {
    val focusManager = LocalFocusManager.current
    val cardVisible = remember { MutableTransitionState(false).apply { targetState = true } }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
            .imePadding()
    ) {
        val minContentHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minContentHeight)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visibleState = cardVisible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 12 }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, AuthCardShape)
                        .background(AuthSurface, AuthCardShape)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content
                )
            }
        }
    }
}

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(text = placeholder, style = MaterialTheme.typography.bodyLarge) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
        singleLine = true,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions,
        trailingIcon = trailingIcon,
        shape = AuthInputShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            errorTextColor = Color.White,
            focusedBorderColor = AuthPrimary,
            unfocusedBorderColor = AuthBorder,
            errorBorderColor = AuthError,
            cursorColor = AuthPrimary,
            focusedPlaceholderColor = AuthTextSecondary,
            unfocusedPlaceholderColor = AuthTextSecondary,
            errorPlaceholderColor = AuthTextSecondary,
            focusedContainerColor = AuthBackground,
            unfocusedContainerColor = AuthBackground,
            errorContainerColor = AuthBackground
        )
    )
}

@Composable
internal fun AuthPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var isVisible by remember { mutableStateOf(false) }
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        modifier = modifier,
        isError = isError,
        keyboardActions = keyboardActions,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isVisible) "Sembunyikan password" else "Tampilkan password",
                    tint = AuthPrimary
                )
            }
        }
    )
}

@Composable
internal fun AuthMessageText(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
internal fun AuthPrimaryButton(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = AuthButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthPrimary,
            contentColor = Color.White,
            disabledContainerColor = AuthPrimary.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
internal fun AuthLinkRow(
    prefix: String,
    action: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = prefix, color = AuthTextSecondary, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onClick, enabled = enabled, contentPadding = PaddingValues(horizontal = 6.dp)) {
            Text(text = action, color = AuthPrimary, style = MaterialTheme.typography.titleSmall)
        }
    }
}
