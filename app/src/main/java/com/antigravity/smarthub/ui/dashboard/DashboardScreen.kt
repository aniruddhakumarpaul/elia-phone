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
import com.antigravity.smarthub.core.model.ActionHistoryRecord
import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.PrivilegeTier
import com.antigravity.smarthub.core.state.ResolvedState
import com.antigravity.smarthub.core.telemetry.TelemetryState
import com.antigravity.smarthub.platform.shizuku.ShizukuState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    deviceState: DeviceState,
    resolvedState: ResolvedState,
    historyLog: List<ActionHistoryRecord> = emptyList(),
    shizukuState: ShizukuState = ShizukuState.DISCONNECTED,
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
                            containerColor = when (shizukuState) {
                                ShizukuState.CONNECTED -> MaterialTheme.colorScheme.primary
                                ShizukuState.CONNECTING -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = shizukuState.name,
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
                val bVal = if (deviceState.batteryPercent.state == TelemetryState.AVAILABLE) "${deviceState.batteryPercent.value}%" else "UNAVAILABLE"
                val isChg = deviceState.isCharging.value ?: false
                val bTemp = if (deviceState.batteryTempC.state == TelemetryState.AVAILABLE) "${deviceState.batteryTempC.value}°C" else "N/A"
                val bSub = if (isChg) "Charging ($bTemp)" else "Discharging ($bTemp)"

                val tVal = if (deviceState.thermalStatus.state == TelemetryState.AVAILABLE) deviceState.thermalStatus.value?.name ?: "UNKNOWN" else "UNAVAILABLE"
                val apTemp = if (deviceState.apTempC.state == TelemetryState.AVAILABLE) "${deviceState.apTempC.value}°C" else "N/A"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        title = "Battery",
                        value = bVal,
                        subtitle = bSub
                    )
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        title = "Thermal Status",
                        value = tVal,
                        subtitle = "AP Temp: $apTemp"
                    )
                }
            }

            // Row 2: Refresh Rate & Memory PSI
            item {
                val refMode = deviceState.activeRefreshRateMode.value
                val refVal = if (deviceState.activeRefreshRateMode.state == TelemetryState.AVAILABLE) {
                    if (refMode == 0) "120 Hz" else "60 Hz"
                } else "UNAVAILABLE"

                val psiVal = if (deviceState.memoryPsiAvg10.state == TelemetryState.AVAILABLE) "%.2f".format(deviceState.memoryPsiAvg10.value ?: 0f) else "N/A"
                val memAvail = if (deviceState.memoryAvailableMb.state == TelemetryState.AVAILABLE) "${deviceState.memoryAvailableMb.value} MB" else "N/A"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        title = "Refresh Rate",
                        value = refVal,
                        subtitle = "Mode: ${refMode ?: "N/A"}"
                    )
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        title = "Memory PSI",
                        value = psiVal,
                        subtitle = "$memAvail Avail"
                    )
                }
            }

            // Row 3: Foreground Package
            item {
                val fgPkg = deviceState.foregroundPackage.value ?: ""
                val fgDisplay = if (fgPkg.isNotBlank()) fgPkg.substringAfterLast('.') else "Background"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        title = "Foreground App",
                        value = fgDisplay,
                        subtitle = if (fgPkg.isNotBlank()) fgPkg else "None"
                    )
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        title = "Privilege Tier",
                        value = deviceState.privilegeTier.name,
                        subtitle = "Shizuku Service"
                    )
                }
            }

            // 3. Recommended Actions
            item {
                Text(
                    text = "Recommended Profile Actions",
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

            // 4. Action History Log (Explainability)
            item {
                Text(
                    text = "Action History & Audit Log",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (historyLog.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "No system action history logged yet",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(historyLog.size) { index ->
                    val record = historyLog[index]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = record.actionId,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = record.capabilityResult.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (record.capabilityResult.name == "SUPPORTED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Profile: ${record.newProfile.name} | Rationale: ${record.triggeringTelemetrySummary}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!record.safetyVetoResult.isAllowed) {
                                Text(
                                    text = "VETO: ${record.safetyVetoResult.vetoReason}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error
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
