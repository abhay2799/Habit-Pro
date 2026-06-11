package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HabitViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: HabitViewModel,
    onOnboardingComplete: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var customName by remember { mutableStateOf("") }

    val stepsContent = listOf(
        OnboardingStep(
            title = "Aesthetic Habit Rituals",
            description = "Welcome to a clutter-free habit space. Transform fragile routines into consistent daily rituals designed with elegant minimalist focus.",
            icon = Icons.Filled.Celebration,
            accentColor = Color(0xFF10B981) // Emerald Green
        ),
        OnboardingStep(
            title = "Streaks & Dynamic Badges",
            description = "Maintain momentum to accumulate XP and in-app Coins. Achieve consistency targets to unlock legendary, high-contrast dynamic medals!",
            icon = Icons.Filled.LocalFireDepartment,
            accentColor = Color(0xFFF59E0B) // Amber
        ),
        OnboardingStep(
            title = "Accountability & Alarms",
            description = "Guard your progress by enabling push reminders. Share updates with the accountability crowd and cheer friends on their healthy journeys.",
            icon = Icons.Filled.NotificationsActive,
            accentColor = Color(0xFF3B82F6) // Slate Blue
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )
            .padding(24.withDotDp())
            .systemBarsPadding()
    ) {
        // Skip Header for Returning / Power users
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛡️ Habit Tracker",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(
                onClick = {
                    viewModel.loginAsGuest()
                    onOnboardingComplete()
                },
                modifier = Modifier.testTag("skip_onboarding_button")
            ) {
                Text("Skip Tutorial", fontWeight = FontWeight.SemiBold)
            }
        }

        // Mid Card Carousel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(vertical = 40.withDotDp()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn() with
                            slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut()
                },
                label = "carousel"
            ) { targetStep ->
                val data = stepsContent[targetStep]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.withDotDp())
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.withDotDp())
                            .clip(CircleShape)
                            .background(data.accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = data.icon,
                            contentDescription = null,
                            tint = data.accentColor,
                            modifier = Modifier.size(48.withDotDp())
                        )
                    }

                    Spacer(modifier = Modifier.height(32.withDotDp()))

                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.withDotDp()))

                    Text(
                        text = data.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.withDotSp(),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.withDotDp()))

            // Step Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.withDotDp()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                stepsContent.forEachIndexed { index, _ ->
                    val width = if (index == step) 28.withDotDp() else 8.withDotDp()
                    val color = if (index == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    Box(
                        modifier = Modifier
                            .height(8.withDotDp())
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            // On Step 2 - Input name to get custom guest setup
            if (step == 2) {
                Spacer(modifier = Modifier.height(24.withDotDp()))
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Enter your name (Optional)") },
                    shape = RoundedCornerShape(12.withDotDp()),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .testTag("user_name_input"),
                    singleLine = true
                )
            }
        }

        // Action Footer (Back/Next/Start buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.withDotDp())
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 0) {
                TextButton(
                    onClick = { step-- }
                ) {
                    Text("Back", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            } else {
                Spacer(modifier = Modifier.width(60.withDotDp()))
            }

            if (step < 2) {
                FilledTonalButton(
                    onClick = { step++ },
                    shape = RoundedCornerShape(16.withDotDp()),
                    modifier = Modifier.testTag("next_onboarding_step_button")
                ) {
                    Text("Next  ", fontWeight = FontWeight.Bold)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.withDotDp()))
                }
            } else {
                Button(
                    onClick = {
                        viewModel.loginAsGuest()
                        if (customName.isNotBlank()) {
                            viewModel.upgradeToCloud(customName + "@guest.local")
                        }
                        onOnboardingComplete()
                    },
                    shape = RoundedCornerShape(16.withDotDp()),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("start_as_guest_button")
                ) {
                    Text("Begin Habit Tracking", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun Int.withDotDp() = this.dp
private fun Int.withDotSp() = this.sp
private fun Double.withDotDp() = this.dp
private fun Float.withDotDp() = this.dp

data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color
)

