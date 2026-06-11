package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdManager
import com.example.services.AmbientSoundService
import androidx.core.content.ContextCompat
import com.example.ui.HabitViewModel

@Composable
fun SoundscapesScreen(viewModel: HabitViewModel) {
    val context = LocalContext.current
    val userSession by viewModel.userSession.collectAsState()

    val soundscapes = listOf(
        Pair("Cosmic Binarual Waves 🎵", "Local File: cosmic_binarual_waves.mp3"),
        Pair("Cozy Rain Sound 🎵", "Local File: cozy_rain_sound.mp3"),
        Pair("Forest Ambient Meditation 🎵", "Local File: forest_ambient_meditation.mp3"),
        Pair("Indian Tanpura 🎵", "Local File: indian_tanpura.mp3"),
        Pair("Mantra OM 🎵", "Local File: mantra_om.mp3"),
        Pair("Night Sound 🎵", "Local File: night_sound.mp3")
    )

    var activeStateIdx by remember { mutableStateOf(-1) }
    var soundVolume by remember { mutableStateOf(0.7f) }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "🎧 Zen Soundscapes",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Text(
            text = "Real-time hardware synthesized noise loops. Perfect for ultimate flow locking and deep work.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicVideo,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (activeStateIdx != -1) "Currently Playing" else "No Active Stream",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (activeStateIdx != -1) soundscapes[activeStateIdx].first else "Stream offline • Select a loop below",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (activeStateIdx != -1) {
                    IconButton(
                        onClick = {
                            activeStateIdx = -1
                            val stopIntent = Intent(context, AmbientSoundService::class.java).apply { action = AmbientSoundService.ACTION_STOP }
                            context.startService(stopIntent)
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Stream",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(soundscapes) { sIdx, pair ->
                val isSelected = activeStateIdx == sIdx
                val hasPremiumAccess = true // Fully unlocked

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable {
                            if (isSelected) {
                                activeStateIdx = -1
                                val stopIntent = Intent(context, AmbientSoundService::class.java).apply { action = AmbientSoundService.ACTION_STOP }
                                context.startService(stopIntent)
                            } else {
                                    activeStateIdx = sIdx
                                    val playIntent = Intent(context, AmbientSoundService::class.java).apply {
                                        action = AmbientSoundService.ACTION_PLAY
                                        putExtra(AmbientSoundService.EXTRA_TRACK_INDEX, sIdx)
                                        putExtra(AmbientSoundService.EXTRA_VOLUME, soundVolume)
                                    }
                                    ContextCompat.startForegroundService(context, playIntent)
                            }
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pair.first,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pair.second,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!hasPremiumAccess) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Sponsor Unlocked Only",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp)
                            )
                        }
                        Icon(
                            imageVector = if (isSelected) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                            contentDescription = "Status Play State Icon Indicator",
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Dedicated volume slider control panel
        if (activeStateIdx != -1) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = if (soundVolume > 0.5f) Icons.Default.VolumeUp else if (soundVolume > 0f) Icons.Default.VolumeDown else Icons.Default.VolumeOff,
                        contentDescription = "Active Volume Controls Slider",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Slider(
                        value = soundVolume,
                        onValueChange = {
                            soundVolume = it
                            val volIntent = Intent(context, AmbientSoundService::class.java).apply {
                                action = AmbientSoundService.ACTION_SET_VOLUME
                                putExtra(AmbientSoundService.EXTRA_VOLUME, it)
                            }
                            context.startService(volIntent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("applet_ambient_volume_slider")
                    )
                }
            }
        }
    }
}
