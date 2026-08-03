package com.martypaz.myq.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.martypaz.myq.R
import com.martypaz.myq.data.prefs.Profile
import com.martypaz.myq.ui.components.StarField
import com.martypaz.myq.ui.components.glass
import com.martypaz.myq.ui.theme.SkyPalette
import kotlinx.coroutines.delay

/**
 * First run, one question at a time: a name, then a postcode, then out of the
 * way. Returning viewers get a short "welcome back" that dismisses itself.
 *
 * The postcode is asked for because it is the only way to know which
 * transmitter region this television receives, and so which listings are the
 * right ones. Both questions can be skipped — a viewer who declines gets a
 * national guide and no greeting, which is worse than the alternative but
 * better than being held at a form.
 */
@Composable
fun WelcomeScreen(
    profile: Profile,
    isProfileLoaded: Boolean,
    needsName: Boolean,
    needsRegion: Boolean,
    isResolvingRegion: Boolean,
    regionError: String?,
    onNameEntered: (String) -> Unit,
    onNameSkipped: () -> Unit,
    onPostcodeEntered: (String) -> Unit,
    onRegionSkipped: () -> Unit,
    onContinue: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF05091C)),
        contentAlignment = Alignment.Center,
    ) {
        StarField(modifier = Modifier.fillMaxSize())

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

            when {
                // A null name means "none saved", which is indistinguishable
                // from "not looked yet" — acting early opened the keyboard at
                // someone the app already knew.
                !isProfileLoaded -> Unit

                needsName -> NamePrompt(onNameEntered = onNameEntered, onSkip = onNameSkipped)

                needsRegion -> PostcodePrompt(
                    firstName = profile.firstName,
                    isResolving = isResolvingRegion,
                    error = regionError,
                    onPostcodeEntered = onPostcodeEntered,
                    onSkip = onRegionSkipped,
                )

                else -> WelcomeBack(profile = profile, onContinue = onContinue)
            }
        }
    }
}

@Composable
private fun WelcomeBack(profile: Profile, onContinue: () -> Unit) {
    Heading(profile.firstName?.let { "Welcome back, $it" } ?: "Welcome back")
    Subheading(
        profile.regionName?.let { "Finding what's new for you in $it…" }
            ?: "Finding what's new for you…",
    )

    // Auto-advance, so a returning viewer never has to press anything.
    LaunchedEffect(Unit) {
        delay(2_200)
        onContinue()
    }
}

@Composable
private fun NamePrompt(onNameEntered: (String) -> Unit, onSkip: () -> Unit) {
    var name by remember { mutableStateOf("") }

    Heading("Welcome to MyQ")
    Subheading("What should we call you?")

    PromptField(
        value = name,
        onValueChange = { name = it.take(24) },
        placeholder = "First name",
        capitalization = KeyboardCapitalization.Sentences,
        onSubmit = { if (name.isNotBlank()) onNameEntered(name.trim()) },
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WelcomeButton(label = "Continue", enabled = name.isNotBlank()) {
            onNameEntered(name.trim())
        }
        WelcomeButton(label = "Skip", onClick = onSkip)
    }
}

@Composable
private fun PostcodePrompt(
    firstName: String?,
    isResolving: Boolean,
    error: String?,
    onPostcodeEntered: (String) -> Unit,
    onSkip: () -> Unit,
) {
    var postcode by remember { mutableStateOf("") }

    Heading(firstName?.let { "Nearly there, $it" } ?: "Nearly there")
    Subheading(
        error
            ?: "Your postcode tells us which transmitter you receive, so the guide " +
            "matches the channels your aerial actually gets.",
    )

    PromptField(
        value = postcode,
        onValueChange = { postcode = it.take(10) },
        placeholder = "Postcode",
        capitalization = KeyboardCapitalization.Characters,
        onSubmit = { if (postcode.isNotBlank()) onPostcodeEntered(postcode.trim()) },
    )

    if (isResolving) {
        Subheading("Looking up your region…")
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WelcomeButton(label = "Continue", enabled = postcode.isNotBlank() && !isResolving) {
            onPostcodeEntered(postcode.trim())
        }
        WelcomeButton(label = "Skip", onClick = onSkip)
    }
}

@Composable
private fun PromptField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    capitalization: KeyboardCapitalization,
    onSubmit: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(placeholder) { focusRequester.requestFocus() }

    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interaction,
        textStyle = TextStyle(
            color = SkyPalette.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
        ),
        cursorBrush = SolidColor(SkyPalette.TextPrimary),
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .glass(focused = isFocused, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (value.isEmpty()) {
                    BasicText(
                        text = placeholder,
                        style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 22.sp),
                    )
                }
                inner()
            }
        },
        modifier = Modifier.focusRequester(focusRequester),
    )
}

@Composable
private fun Heading(text: String) {
    BasicText(
        text = text,
        style = TextStyle(
            color = SkyPalette.TextPrimary,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
}

@Composable
private fun Subheading(text: String) {
    BasicText(
        text = text,
        style = TextStyle(color = SkyPalette.TextSecondary, fontSize = 17.sp, lineHeight = 24.sp),
    )
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
