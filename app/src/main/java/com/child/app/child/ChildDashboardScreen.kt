package com.child.app.child

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.child.app.R
import com.child.app.ProfileActivity

// Pixel-perfect color palette
val GhostGray = Color(0xFFF2F4F7)
val TextPrimary = Color(0xFF1D1B20)
val TextSecondary = Color(0xFF49454F)
val TextTertiary = Color(0xFF938F99)
val AppGreen = Color(0xFF00C853)
val BarBackground = Color(0xFFF2F4F7)

enum class DashboardTab {
    Stats, Risks, Location, Profile
}

@Composable
fun ChildDashboardScreen(viewModel: ChildDashboardViewModel = viewModel()) {
    var currentTab by remember { mutableStateOf(DashboardTab.Stats) }
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = GhostGray
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            HeaderSection(viewModel, currentTab) { tab ->
                when (tab) {
                    DashboardTab.Risks -> {
                        context.startActivity(Intent(context, MaliciousAppActivity::class.java))
                    }
                    DashboardTab.Location -> {
                        context.startActivity(Intent(context, ChildLocationActivity::class.java))
                    }
                    DashboardTab.Stats -> {
                        currentTab = DashboardTab.Stats
                    }
                    DashboardTab.Profile -> {
                        context.startActivity(Intent(context, ProfileActivity::class.java))
                    }
                }
            }
            
            Crossfade(targetState = currentTab, label = "TabTransition") { tab ->
                when (tab) {
                    DashboardTab.Stats -> {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            StatsCard(
                                viewModel, 
                                onRiskClick = { context.startActivity(Intent(context, MaliciousAppActivity::class.java)) }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    else -> { /* Risks and Location handled by Activities */ }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(
    viewModel: ChildDashboardViewModel, 
    currentTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit
) {
    val name by viewModel.userName
    val letter by viewModel.avatarLetter
    val color by viewModel.avatarColor
    val profileImage by viewModel.profileImageUrl

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Profile Tab
        Row(
            modifier = Modifier
                .offset(x = 3.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(4.dp)
                .clickable { onTabSelected(DashboardTab.Profile) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (profileImage == null) color else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (profileImage != null) {
                    AsyncImage(
                        model = profileImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(letter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action Buttons
        Row(
            modifier = Modifier.offset(x = (-3).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderIconButton(
                iconRes = R.drawable.ic_risk, 
                isSelected = currentTab == DashboardTab.Risks
            ) { onTabSelected(DashboardTab.Risks) }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            HeaderIconButton(
                iconRes = R.drawable.location, 
                isSelected = currentTab == DashboardTab.Location
            ) { onTabSelected(DashboardTab.Location) }
        }
    }
}

@Composable
fun HeaderIconButton(iconRes: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isSelected) TextPrimary else Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected) Color.White else TextPrimary
        )
    }
}

@Composable
fun StatsCard(
    viewModel: ChildDashboardViewModel, 
    onRiskClick: () -> Unit
) {
    val screenTime by viewModel.screenTimeToday
    val lastUpdated by viewModel.lastUpdated
    val comparison by viewModel.comparisonText
    val hourlyData = viewModel.hourlyUsage
    val appsData = viewModel.topAppsUsage

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Screen time header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Screen time today", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(lastUpdated, color = TextTertiary, fontSize = 12.sp)
            }
            
            // Time and Toggle
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(screenTime, color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Black)
                
                // Toggle
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(GhostGray)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("DAY", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("WEEK", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Text(comparison, color = TextTertiary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))

            Spacer(modifier = Modifier.height(32.dp))
            
            // By Hour header with Risk Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("By hour", color = TextSecondary, fontSize = 12.sp)
                Icon(
                    painter = painterResource(id = R.drawable.ic_risk),
                    contentDescription = "Open Risks",
                    tint = TextTertiary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onRiskClick() }
                )
            }
            
            // Hourly Graph
            Box(modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 12.dp)) {
                Row(modifier = Modifier.fillMaxSize().padding(end = 40.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    val maxVal = (hourlyData.maxOrNull() ?: 1f).coerceAtLeast(1f)
                    repeat(24) { i ->
                        val value = hourlyData.getOrNull(i) ?: 0f
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight(0.85f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(BarBackground)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(value / maxVal)
                                    .align(Alignment.BottomCenter)
                                    .background(AppGreen)
                            )
                        }
                    }
                }
                // Labels
                Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(end = 40.dp)) {
                    Text("0:00", color = TextTertiary, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Text("12:00", color = TextTertiary, fontSize = 10.sp, modifier = Modifier.weight(1f))
                }
                // Right side Y-labels
                Column(modifier = Modifier.align(Alignment.CenterEnd), horizontalAlignment = Alignment.End) {
                    Text("40 min", color = TextTertiary, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(45.dp))
                    Text("20 min", color = TextTertiary, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(45.dp))
                    Text("0 min", color = TextTertiary, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("By app", color = TextSecondary, fontSize = 12.sp)

            // App Graph
            Box(modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 12.dp)) {
                Row(modifier = Modifier.fillMaxSize().padding(end = 40.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    val maxAppVal = (appsData.maxOfOrNull { it.minutes } ?: 1f).coerceAtLeast(1f)
                    repeat(6) { i ->
                        val appUsage = appsData.getOrNull(i)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BarBackground)
                            ) {
                                appUsage?.let {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(it.minutes / maxAppVal)
                                            .align(Alignment.BottomCenter)
                                            .background(AppGreen)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            // App Icons
                            appUsage?.icon?.let { icon ->
                                Image(
                                    bitmap = icon.toBitmap().asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } ?: Box(modifier = Modifier.size(24.dp).background(Color.Transparent))
                        }
                    }
                }
                // Right side Y-labels
                Column(modifier = Modifier.align(Alignment.CenterEnd), horizontalAlignment = Alignment.End) {
                    val maxLabel = if (appsData.isNotEmpty()) "${(appsData[0].minutes).toInt()} min" else "20 min"
                    Text(maxLabel, color = TextTertiary, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(45.dp))
                    Text("10 min", color = TextTertiary, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(45.dp))
                    Text("0 min", color = TextTertiary, fontSize = 10.sp)
                }
            }
        }
    }
}
