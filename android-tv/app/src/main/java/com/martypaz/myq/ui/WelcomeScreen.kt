package com.martypaz.myq.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.martypaz.myq.R
import com.martypaz.myq.ui.components.StarField
import com.martypaz.myq.ui.theme.SkyPalette
import kotlinx.coroutines.delay
import com.martypaz.myq.ui.components.glass

/**
 * First thing MyQ shows: the parallax starfield behind either a name prompt
 * (first run) or a short "Welcome back" that dismisses itself.
 */
@Composable
fun WelcomeScreen(
    firstName: String?,
    onNameEntered: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05091C)),
        contentAlignment = Alignment.Center,
    ) {
        StarField(modifier = Modifier.fillMaxSize())

        // The starfield is the backdrop here; the panel below is the glass.

        val fade by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700),
            label = "welcomeFade",
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .alpha(fade)
                .glass(shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 64.dp, vertical = 44.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_myq_logo),
                contentDescription = "MyQ",
                modifier = Modifier.size(96.dp),
            )

            if (firstName == null) {
                NamePrompt(onNameEntered = onNameEntered)
            } else {
                WelcomeBack(firstName = firstName, onContinue = onContinue)
            }
        }
    }
}

@Composable
private fun WelcomeBack(firstName: String, onContinue: () -> Unit) {
    BasicText(
        text = "Welcome back, $firstName",
        style = TextStyle(
            color = SkyPalette.TextPrimary,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
    BasicText(
        text = "Finding what's new for you…",
        style = TextStyle(color = SkyPalette.TextSecondary, fontSize = 17.sp),
    )

    // Auto-advance, so a returning viewer never has to press anything.
    LaunchedEffect(firstName) {
        delay(2_200)
        onContinue()
    }
}

@Composable
private fun NamePrompt(onNameEntered: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicText(
        text = "Welcome to MyQ",
        style = TextStyle(
            color = SkyPalette.TextPrimary,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
    BasicText(
        text = "What should we call you?",
        style = TextStyle(color = SkyPalette.TextSecondary, fontSize = 17.sp),
    )

    val fieldInteraction = remember { MutableInteractionSource() }
    val fieldFocused by fieldInteraction.collectIsFocusedAsState()

    BasicTextField(
        value = name,
        onValueChange = { name = it.take(24) },
        singleLine = true,
        interactionSource = fieldInteraction,
        textStyle = TextStyle(
            color = SkyPalette.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
        ),
        cursorBrush = SolidColor(SkyPalette.TextPrimary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { submit(name, onNameEntered) }),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .glass(focused = fieldFocused, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (name.isEmpty()) {
                    BasicText(
                        text = "First name",
                        style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 22.sp),
                    )
                }
                inner()
            }
        },
        modifier = Modifier.focusRequester(focusRequester),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WelcomeButton(label = "Continue", enabled = name.isNotBlank()) {
            submit(name, onNameEntered)
        }
        WelcomeButton(label = "Skip") { onNameEntered("") }
    }
}

private fun submit(name: String, onNameEntered: (String) -> Unit) {
    if (name.isNotBlank()) onNameEntered(name.trim())
}

@Composable
private fun WelcomeButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    BasicText(
        text = label,
        style = TextStyle(
            color = when {
                !enabled -> SkyPalette.TextTertiary
                isFocused -> Color(0xFF060B1D)
                else -> SkyPalette.TextPrimary
            },
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        modifier = Modifier
            .then(
                if (isFocused && enabled) {
                    Modifier.background(SkyPalette.TextPrimary, RoundedCornerShape(24.dp))
                } else {
                    Modifier.glass(shape = RoundedCornerShape(24.dp))
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 26.dp, vertical = 12.dp),
    )
}
