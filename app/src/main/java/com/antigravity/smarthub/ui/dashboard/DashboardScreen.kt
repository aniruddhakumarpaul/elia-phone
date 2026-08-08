package com.antigravity.smarthub.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.PrivilegeTier
import com.antigravity.smarthub.core.state.ResolvedState
import com.antigravity.smarthub.core.telemetry.DeviceTelemetrySnapshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    deviceState: DeviceState,
    resolvedState: ResolvedState,
    telemetrySnapshot: DeviceTelemetrySnapshot? = null,
    onRefresh: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SMART HUB",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Badge(
                            containerColor = if (deviceState.privilegeTier == PrivilegeTier.TIER_1_SHIZUKU) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = if (deviceState.privilegeTier == PrivilegeTier.TIER_1_SHIZUKU) "SHIZUKU" else "STOCK",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onRefresh) {
                        Text("REFRESH", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Active Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ACTIVE PROFILE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = resolvedState.activeProfile.displayName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resolvedState.rationale,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 2. Hardware Telemetry Metrics Header
            item {
                Text(
                    text = "Live Telemetry",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Row 1: Battery & Thermal
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        title = "Battery",
                        value = "${deviceState.batteryPercent}%",
                        subtitle = if (deviceState.isCharging) "Charging (${deviceState.batteryTempC}°C)" else "Discharging (${deviceState.batteryTempC}°C)"
                    )
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        title = "Thermal Status",
                        value = deviceState.thermalStatus.name,
                        subtitle = "AP Temp: ${deviceState.apTempC}°C"
                    )
                }
            }

            // Row 2: Refresh Rate & Memory PSI
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        title = "Refresh Rate",
                        value = if (deviceState.activeRefreshRateMode == 0) "120 Hz" else "60 Hz",
                        subtitle = "Mode: ${deviceState.activeRefreshRateMode}"
                    )
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        title = "Memory PSI",
                        value = "%.2f".format(deviceState.memoryPsiAvg10),
                        subtitle = "${deviceState.memoryAvailableMb} MB Avail"
                    )
                }
            }

            // Row 3: CPU & Foreground Package
            item {
                val fgDisplay = if (deviceState.foregroundPackage.isNotBlank()) {
                    deviceState.foregroundPackage.substringAfterLast('.')
                } else {
                    "None"
                }

                val cpuCores = telemetrySnapshot?.cpuMetrics?.value
                val onlineCores = cpuCores?.count { it.isOnline } ?: 8
                val totalCores = cpuCores?.size ?: 8

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        title = "CPU Cores",
                        value = "$onlineCores / $totalCores Online",
                        subtitle = "Exynos 1280 (6+2)"
                    )
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        title = "Foreground App",
                        value = fgDisplay,
                        subtitle = if (deviceState.foregroundPackage.isNotBlank()) deviceState.foregroundPackage else "Background"
                    )
                }
            }

            // 3. Recommended Actions
            item {
                Text(
                    text = "Applied / Recommended Actions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (resolvedState.recommendedActions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "No pending actions for current state",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(resolvedState.recommendedActions.size) { index ->
                    val action = resolvedState.recommendedActions[index]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = action.actionId,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = action.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Badge(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = action.requiredTier.name,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}
