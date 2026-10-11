package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.ui.theme.*

@Composable
fun StatusBadge(
    state: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (state) {
        ConnectionState.ONLINE -> Triple(StatusOnlineGreen.copy(alpha = 0.2f), StatusOnlineGreen, "ONLINE")
        ConnectionState.OFFLINE -> Triple(StatusOfflineGray.copy(alpha = 0.2f), StatusOfflineGray, "OFFLINE")
        ConnectionState.CONNECTING -> Triple(StatusConnectingYellow.copy(alpha = 0.2f), StatusConnectingYellow, "CONNECTING")
        ConnectionState.ERROR -> Triple(StatusErrorRed.copy(alpha = 0.2f), StatusErrorRed, "ERROR")
    }

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ConnectionCard(
    title: String,
    ip: String,
    port: Int,
    state: ConnectionState,
    pingMs: Long,
    lastError: String?,
    icon: ImageVector,
    onTestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CharcoalSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ForestGreenLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextWhitePrimary
                        )
                        Text(
                            text = "$ip:$port",
                            fontSize = 12.sp,
                            color = TextMutedSecondary
                        )
                    }
                }
                StatusBadge(state = state)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (state == ConnectionState.ONLINE) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = StatusOnlineGreen, modifier = Modifier.size(14.dp))
                        Text(text = "Latency: ${pingMs}ms", fontSize = 12.sp, color = StatusOnlineGreen)
                    }
                } else if (!lastError.isNullOrBlank()) {
                    Text(
                        text = lastError,
                        fontSize = 11.sp,
                        color = StatusErrorRed,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(text = "Standby (Awaiting response)", fontSize = 12.sp, color = TextMutedSecondary)
                }

                OutlinedButton(
                    onClick = onTestClick,
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("test_btn_${title.take(4).lowercase()}"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Test", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun TankLevelBar(
    title: String,
    levelPct: Float?,
    minThresholdPct: Float,
    barColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icon, contentDescription = null, tint = barColor, modifier = Modifier.size(18.dp))
                    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhitePrimary)
                }
                Text(
                    text = if (levelPct != null) "${levelPct.toInt()}%" else "Unavailable",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (levelPct == null) TextMutedSecondary else if (levelPct < minThresholdPct) StatusErrorRed else TextWhitePrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val progress = (levelPct ?: 0f) / 100f
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if ((levelPct ?: 0f) < minThresholdPct) StatusErrorRed else barColor,
                trackColor = CharcoalSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Safety min: ${minThresholdPct.toInt()}%", fontSize = 10.sp, color = TextMutedSecondary)
                if (levelPct != null && levelPct < minThresholdPct) {
                    Text(text = "LOW LEVEL WARNING", fontSize = 10.sp, color = StatusErrorRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
