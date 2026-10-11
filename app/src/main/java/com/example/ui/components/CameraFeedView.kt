package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.model.WeedClass
import com.example.ui.theme.*
import com.example.vision.DetectionResult

@Composable
fun CameraFeedView(
    frame: Bitmap?,
    connectionState: ConnectionState,
    errorMessage: String?,
    detection: DetectionResult?,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (frame != null && connectionState == ConnectionState.ONLINE) {
                // Real camera frame
                Image(
                    bitmap = frame.asImageBitmap(),
                    contentDescription = "Real ESP32-CAM live feed",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Detection bounding box overlay (only rendered when real inference has produced a box)
                if (detection?.boundingBox != null) {
                    val box = detection.boundingBox
                    val boxColor = when (detection.classification) {
                        WeedClass.WEED -> WeedAlertRed
                        WeedClass.ONION -> OnionSafeGreen
                        WeedClass.UNKNOWN -> UnknownGray
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val left = box.left * size.width
                        val top = box.top * size.height
                        val width = (box.right - box.left) * size.width
                        val height = (box.bottom - box.top) * size.height

                        // Draw bounding box
                        drawRect(
                            color = boxColor,
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Draw center target crosshair
                        val centerX = detection.centerNormX * size.width
                        val centerY = detection.centerNormY * size.height
                        val crosshairSize = 12.dp.toPx()

                        drawLine(
                            color = boxColor,
                            start = Offset(centerX - crosshairSize, centerY),
                            end = Offset(centerX + crosshairSize, centerY),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = boxColor,
                            start = Offset(centerX, centerY - crosshairSize),
                            end = Offset(centerX, centerY + crosshairSize),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Detection label pill
                    Surface(
                        color = boxColor.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "${detection.classification.name}: ${(detection.confidence * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            } else {
                // Honest offline / disconnected state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = null,
                        tint = StatusOfflineGray,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        text = if (connectionState == ConnectionState.ERROR) "ESP32-CAM Disconnected" else "No Camera Frames Received",
                        color = TextWhitePrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = errorMessage ?: "Verify ESP32-CAM power, Wi-Fi IP address and capture endpoint in Settings.",
                        color = TextMutedSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Bottom bar with controls
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (connectionState == ConnectionState.ONLINE) StatusOnlineGreen else StatusOfflineGray)
                    )
                    Text(
                        text = if (connectionState == ConnectionState.ONLINE) "ESP32-CAM LIVE" else "OFFLINE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onCaptureClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("cam_capture_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture still frame",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
