package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HabitViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val session by viewModel.userSession.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    val scrollState = rememberScrollState()

    var showReceiptDialog by remember { mutableStateOf(false) }
    var receiptContent by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    fun processRevenueCatPurchase(productId: String, isPremiumUpgrade: Boolean, coinsToCredit: Int, priceStr: String) {
        // Simulating the Google Play Billing success since we don't have live products in this sandbox:
        
        isProcessing = true
        
        // This is where standard Google Play Billing (via direct card / stored card)
        // appears for the user.
        // In a real app with RevenueCat products configured, you fetch Offerings first:
        /*
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error -> isProcessing = false },
            onSuccess = { offerings ->
                val pkg = offerings.current?.availablePackages?.firstOrNull { it.product.id == productId }
                if (pkg != null && activity != null) {
                    Purchases.sharedInstance.purchase(
                        PurchaseParams.Builder(activity, pkg).build(),
                        object : PurchaseCallback { ... }
                    )
                }
            }
        )
        */

        // Simulating the Google Play Billing success since we don't have live products in this sandbox:
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            isProcessing = false
            if (isPremiumUpgrade) {
                viewModel.checkoutPremiumUpgrade()
            }
            if (coinsToCredit > 0) {
                viewModel.purchaseInAppPayment(coinsToCredit, 0.0)
            }
            receiptContent = "SUCCESS_BUNDLE_${productId}_$priceStr"
            showReceiptDialog = true
        }, 1200)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 70.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shop Title
        Text(
            text = "Prestige Store",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Wallet Card
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "In-App Gold Balance",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${session.coins}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🪙",
                            fontSize = 28.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (session.isPremium) "👑 Premium Active" else "🛡️ Standard Tier",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Inner spacing between wallet card and Go Pro account offer
        Spacer(modifier = Modifier.height(4.dp))

        // --- SECTION: Go Pro Account Offer ---
        if (!session.isPremium) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Upgrade to Habit Pro 👑",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        "Unlock advanced tracking blueprints built for long-term consistency:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BulletPoint("Unlimited simultaneous Habit Trackers")
                        BulletPoint("Access 30-Day 'Habits Architect' Medal")
                        BulletPoint("Interactive Cloud Synchronizer storage backup")
                        BulletPoint("Completely remove advertising space")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Option A: Buy with Gold coins
                        Button(
                            onClick = {
                                val success = viewModel.checkoutPremiumUpgrade()
                                if (success) {
                                    receiptContent = "PRO_COIN_CHECKOUT_150"
                                    showReceiptDialog = true
                                } else {
                                    Toast.makeText(context, "Insufficient Gold Coins. Buy more below!", Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f).testTag("buy_premium_coins_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Upgrade (150 🪙)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Option B: Buy with Play Store
                        Button(
                            onClick = {
                                processRevenueCatPurchase("premium_pro_upgrade", true, 0, "1.99")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onTertiary)
                            } else {
                                Text("Buy Pro ($1.99)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(32.dp))
                    Column {
                        Text("Pro Tier Activated", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text("Enjoy unlimited tracking blueprints, elite badges, and full cloud syncing backups.", fontSize = 11.sp)
                    }
                }
            }
        }

        // --- SECTION: Virtual Power-Ups & Booster Shields ---
        Text(
            "Power-Ups & Streak Shields",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        val activeFreezers by viewModel.streakFreezerCount.collectAsState()
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛡️", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Streak Freeze Shield", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text("Active Shields Owned: $activeFreezers", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Button(
                        onClick = {
                            val ok = viewModel.buyStreakFreezer()
                            if (ok) {
                                Toast.makeText(context, "🎉 Shield purchased! Protected from missed streaks.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Insufficient Gold coins! Level up or buy packs.", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Buy (50 🪙)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "Shields prevent active habit streaks from dropping back down to 0 if you forget to complete them today. Spent automatically inside card detail to restore consistency!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- SECTION: Coins Packages Store (Paid Items) ---
        Text(
            "Gold Coin Store packs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        CoinPackRow(
            packName = "Handful of Gold Coins",
            coinsAmount = 100,
            costText = "$0.99",
            iconEmoji = "🪙",
            tintColor = Color(0xFF10B981),
            onBuyClick = {
                processRevenueCatPurchase("gold_coins_100", false, 100, "0.99")
            }
        )

        CoinPackRow(
            packName = "Vault of Gold Coins",
            coinsAmount = 500,
            costText = "$2.99",
            iconEmoji = "🏺",
            tintColor = Color(0xFF3B82F6),
            onBuyClick = {
                processRevenueCatPurchase("gold_coins_500", false, 500, "2.99")
            }
        )

        CoinPackRow(
            packName = "Royal Treasure Chest",
            coinsAmount = 1200,
            costText = "$5.99",
            iconEmoji = "📦",
            tintColor = Color(0xFF8B5CF6),
            onBuyClick = {
                processRevenueCatPurchase("gold_coins_1200", false, 1200, "5.99")
            }
        )
    }

    // --- DIALOG: Purchase Success Receipt ---
    if (showReceiptDialog) {
        AlertDialog(
            onDismissRequest = { showReceiptDialog = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Order Successful", fontWeight = FontWeight.Black)
                }
            },
            text = {
                Text(
                    text = when {
                        receiptContent.startsWith("SUCCESS_BUNDLE") -> {
                            val parts = receiptContent.split("_")
                            val name = parts.getOrNull(2) ?: "Bundle"
                            val price = parts.getOrNull(3) ?: "pack"
                            "You have successfully purchased: '$name' worth $$price USD.\n\nYour Gold balance has been credited and cloud backends are updated!"
                        }
                        else -> "Your Habit Pro Premium Account upgrade has been successfully unlocked. Custom 30-day tracking, Pro medals, and Cloud synchronization features are now instantly activated!"
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showReceiptDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("receipt_done_button")
                ) {
                    Text("Fantastic")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFF59E0B))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

@Composable
fun CoinPackRow(
    packName: String,
    coinsAmount: Int,
    costText: String,
    iconEmoji: String,
    tintColor: Color,
    onBuyClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(tintColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(iconEmoji, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(packName, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Text("Credited: +$coinsAmount Gold coins instantly", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Buy button
            Button(
                onClick = onBuyClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                modifier = Modifier.testTag("buy_pack_btn_${coinsAmount}")
            ) {
                Text(costText, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}
