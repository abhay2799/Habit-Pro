package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.services.AmbientSoundService
import androidx.core.content.ContextCompat
import com.example.ui.HabitViewModel

data class SoundscapeItem(
    val title: String,
    val subtitle: String,
    val drawableRes: Int
)

@Composable
fun SoundscapesScreen(viewModel: HabitViewModel) {
    val context = LocalContext.current

    val soundscapes = remember {
        listOf(
            SoundscapeItem("Cosmic Binaural Waves 🎵", "Local File: cosmic_binaural_waves.mp3", R.drawable.cosmic_binaural_waves),
            SoundscapeItem("Cozy Rain Sound 🎵", "Local File: cozy_rain_sound.mp3", R.drawable.cozy_rain_sound),
            SoundscapeItem("Forest Ambient Meditation 🎵", "Local File: forest_ambient_meditation.mp3", R.drawable.forest_ambient_meditation),
            SoundscapeItem("Indian Tanpura 🎵", "Local File: indian_tanpura.mp3", R.drawable.indian_tanpura),
            SoundscapeItem("Mantra OM 🎵", "Local File: mantra_om.mp3", R.drawable.mantra_om),
            SoundscapeItem("Night Sound 🎵", "Local File: night_sound.mp3", R.drawable.night_sound)
        )
    }

    val activeStateIdx by viewModel.activeSoundscapeIdx.collectAsState()
    val soundVolume by viewModel.soundVolume.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            // No bottom padding — LazyColumn with weight(1f) pushes volume panel to bottom naturally
    ) {
        // ═══════════════════════════════
        // HEADER: Devlance Studio branding
        // ═══════════════════════════════
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row {
                    Text(
                        text = "Devlance ",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Studio",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
        Text(
            text = "Escape. Focus. Flow. Enjoy offline sounds with\nyour headphones, close your eyes and feel the vibe.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ═══════════════════════════════
        // CURRENTLY PLAYING CARD
        // ═══════════════════════════════
        AnimatedVisibility(
            visible = activeStateIdx != -1,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (activeStateIdx in soundscapes.indices) {
                val activeSoundscape = soundscapes[activeStateIdx]
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Preview thumbnail
                        Image(
                            painter = painterResource(id = activeSoundscape.drawableRes),
                            contentDescription = activeSoundscape.title,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CURRENTLY PLAYING",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeSoundscape.title,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Stop button
                        IconButton(
                            onClick = {
                                viewModel.setActiveSoundscapeIdx(-1)
                                val stopIntent = Intent(context, AmbientSoundService::class.java).apply {
                                    action = AmbientSoundService.ACTION_STOP
                                }
                                context.startService(stopIntent)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
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
        }

        // ═══════════════════════════════
        // SOUNDSCAPE LIST
        // ═══════════════════════════════
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(soundscapes) { sIdx, soundscape ->
                val isSelected = activeStateIdx == sIdx

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isSelected) {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            } else {
                                Modifier
                            }
                        )
                        .clickable {
                            if (isSelected) {
                                viewModel.setActiveSoundscapeIdx(-1)
                                val stopIntent = Intent(context, AmbientSoundService::class.java).apply {
                                    action = AmbientSoundService.ACTION_STOP
                                }
                                context.startService(stopIntent)
                            } else {
                                viewModel.setActiveSoundscapeIdx(sIdx)
                                val playIntent = Intent(context, AmbientSoundService::class.java).apply {
                                    action = AmbientSoundService.ACTION_PLAY
                                    putExtra(AmbientSoundService.EXTRA_TRACK_INDEX, sIdx)
                                    putExtra(AmbientSoundService.EXTRA_VOLUME, soundVolume)
                                }
                                ContextCompat.startForegroundService(context, playIntent)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Preview thumbnail image
                        Image(
                            painter = painterResource(id = soundscape.drawableRes),
                            contentDescription = soundscape.title,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        // Title and subtitle
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = soundscape.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Play/Pause button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                contentDescription = if (isSelected) "Pause" else "Play",
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════
        // VOLUME CONTROL PANEL
        // ═══════════════════════════════
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = if (soundVolume > 0.5f) Icons.Default.VolumeUp
                    else if (soundVolume > 0f) Icons.Default.VolumeDown
                    else Icons.Default.VolumeOff,
                    contentDescription = "Volume",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Slider(
                    value = soundVolume,
                    onValueChange = {
                        viewModel.setSoundVolume(it)
                        if (activeStateIdx != -1) {
                            val volIntent = Intent(context, AmbientSoundService::class.java).apply {
                                action = AmbientSoundService.ACTION_SET_VOLUME
                                putExtra(AmbientSoundService.EXTRA_VOLUME, it)
                            }
                            context.startService(volIntent)
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("applet_ambient_volume_slider")
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = "Equalizer",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
