package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notifications.NotificationHelper
import com.example.ui.HabitViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val habits by viewModel.habitsList.collectAsState()
    val completionsGrouped by viewModel.habitCompletions.collectAsState()
    val userSession by viewModel.userSession.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    val totalHabits = habits.size
    val totalCompletions = completionsGrouped.values.sumOf { it.size }
    val maxStreak = if (habits.isEmpty()) 0 else habits.maxOf { it.topStreak }

    val past30DaysCompletions = remember(habits, completionsGrouped) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val shortDatePrint = SimpleDateFormat("MMM dd", Locale.US)
        (0..29).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            val dateStr = dateFormat.format(cal.time)
            val printLabel = shortDatePrint.format(cal.time)
            val completed = habits.count { completionsGrouped[it.id]?.contains(dateStr) == true }
            Pair(printLabel, completed)
        }.reversed()
    }

    val scrollState = rememberScrollState()

    // Calculate completions by category for charting
    val categoryStats = remember(habits, completionsGrouped) {
        val map = mutableMapOf<String, Int>()
        for (habit in habits) {
            val count = completionsGrouped[habit.id]?.size ?: 0
            map[habit.category] = (map[habit.category] ?: 0) + count
        }
        map
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 70.dp), // gap for Bottom navigation
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Title Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Analytics ",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Hub",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Aggregated Metrics Cards Grid (FlowRow)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Total Checked",
                value = "$totalCompletions",
                icon = Icons.Default.CheckCircle,
                tint = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Top Streak",
                value = "$maxStreak days",
                icon = Icons.Default.LocalFireDepartment,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Active Habits",
                value = "$totalHabits",
                icon = Icons.Default.TrackChanges,
                tint = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "User Level",
                value = "Lvl ${1 + (userSession.totalXp / 100)}",
                icon = Icons.Default.TrendingUp,
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f)
            )
        }

        // --- SECTION: Interactive Canvas-drawn Charts ---
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Habit Category Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                Text(
                    text = "Completion ratios across standard tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                if (categoryStats.isEmpty() || categoryStats.values.all { it == 0 }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Complete some habits to view graphical breakdowns.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Donut Chart on Canvas
                        Canvas(
                            modifier = Modifier
                                .size(140.dp)
                                .weight(1f)
                        ) {
                            val total = categoryStats.values.sum().toFloat()
                            var startAngle = 0f
                            fun getCategoryColor(category: String) = when(category) {
                                "Health" -> Color(0xFF10B981)
                                "Work" -> Color(0xFF8B5CF6)
                                "Personal" -> Color(0xFF3B82F6)
                                "Fitness" -> Color(0xFFEC4899)
                                "Productivity" -> Color(0xFFF59E0B)
                                "Wellness" -> Color(0xFF06B6D4)
                                else -> Color(0xFF1DD75B)
                            }

                            categoryStats.entries.forEachIndexed { i, entry ->
                                val sweepAngle = (entry.value / total) * 360f
                                drawArc(
                                    color = getCategoryColor(entry.key),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    size = Size(size.width, size.height),
                                    style = Stroke(width = 30f)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Legend
                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            fun getCategoryColor(category: String) = when(category) {
                                "Health" -> Color(0xFF10B981)
                                "Work" -> Color(0xFF8B5CF6)
                                "Personal" -> Color(0xFF3B82F6)
                                "Fitness" -> Color(0xFFEC4899)
                                "Productivity" -> Color(0xFFF59E0B)
                                "Wellness" -> Color(0xFF06B6D4)
                                else -> Color(0xFF1DD75B)
                            }
                            categoryStats.entries.forEachIndexed { i, entry ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(getCategoryColor(entry.key))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${entry.key}: ${entry.value}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- FEATURE: 📊 WEEKLY ACTIVITY CHART (RECHARTS ALTERNATIVE) ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "📊 Weekly Activity Trends",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Habits completed over the last 7 days",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val dayFormat = remember { java.text.SimpleDateFormat("EEE", java.util.Locale.US) }
                val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US) }
                
                val chartData = remember(completionsGrouped) {
                    val cal = java.util.Calendar.getInstance()
                    val labels = mutableListOf<String>()
                    val values = mutableListOf<Int>()
                    for (i in 6 downTo 0) {
                        cal.time = java.util.Date()
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
                        val dateString = dateFormat.format(cal.time)
                        val displayDay = dayFormat.format(cal.time)
                        labels.add(if (i == 0) "Today" else displayDay)
                        
                        val count = completionsGrouped.values.count { dates -> dates.contains(dateString) }
                        values.add(count)
                    }
                    Pair(labels, values)
                }
                
                val chartLabels = chartData.first
                val chartValues = chartData.second
                val maxVal = (chartValues.maxOrNull() ?: 1).coerceAtLeast(1)
                val barColor = MaterialTheme.colorScheme.primary

                Row(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    chartValues.forEachIndexed { index, value ->
                        val heightFactor = (value.toFloat() / maxVal).coerceIn(0f, 1f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = value.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height((maxOf(10f, heightFactor * 100f)).dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(if (index == 6) barColor else barColor.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = chartLabels[index],
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (index == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION: 1-Month Completion History Heatmap (1 Month Data) ---
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "30-Day Completion Matrix",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Comprehensive 1-month historic engagement metrics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Past Month",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Heatmap layout representing days
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 6,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    past30DaysCompletions.forEach { (label, completedCount) ->
                        val ratio = if (totalHabits > 0) completedCount.toFloat() / totalHabits.toFloat() else 0f
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        ratio >= 0.8f -> Color(0xFF10B981) // bright emerald
                                        ratio >= 0.4f -> Color(0xFF10B981).copy(alpha = 0.6f) // semi-green
                                        ratio > 0.0f -> Color(0xFF10B981).copy(alpha = 0.25f) // faint green
                                        else -> MaterialTheme.colorScheme.surfaceVariant // empty gray
                                    }
                                )
                                .border(
                                    width = 0.5.dp, 
                                    color = MaterialTheme.colorScheme.outlineVariant, 
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label.substringAfter(" "),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ratio > 0f) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$completedCount check",
                                    fontSize = 8.sp,
                                    color = if (ratio > 0f) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Less check-ins ",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF10B981).copy(alpha = 0.25f)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF10B981).copy(alpha = 0.6f)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF10B981)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = " More check-ins",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun shareFile(context: Context, file: File?, mimeType: String, chooserTitle: String) {
    if (file == null) {
        Toast.makeText(context, "Failed to compile report. Please try again.", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        // Safe, seamless sharing utilizing temporary package local file Uri configurations
        val fileUri = Uri.fromFile(file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    } catch (e: Exception) {
        Toast.makeText(context, "Export saved to secure cache: ${file.name}", Toast.LENGTH_LONG).show()
    }
}
