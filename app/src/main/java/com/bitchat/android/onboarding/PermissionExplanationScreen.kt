package com.bitchat.android.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.R
import com.bitchat.android.ui.theme.BitchatFontFamily

/**
 * Permission explanation screen shown before requesting permissions
 * Explains why SOSBlu needs each permission and reassures users about privacy
 */
@Composable
fun PermissionExplanationScreen(
    modifier: Modifier,
    permissionCategories: List<PermissionCategory>,
    onContinue: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val accentRed = Color(0xFFE53935)
    val accentRedDark = Color(0xFFB71C1C)

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF070B14),
                    Color(0xFF0D1424),
                    Color(0xFF060912)
                )
            )
        )
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 96.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            // Header Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(10.dp, RoundedCornerShape(12.dp), spotColor = accentRed)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accentRed, accentRedDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "SOSBlu",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = BitchatFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color(0xFFF8FAFC)
                    )
                    Text(
                        text = "CONFIGURACIÓN INICIAL DE SENSORES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentRed,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Privacy assurance card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF131B2E)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0x3338BDF8))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = stringResource(R.string.cd_privacy_protected),
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.privacy_protected),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.privacy_bullets),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = BitchatFontFamily,
                                color = Color(0xFF94A3B8),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Section header
            Text(
                text = "PERMISOS OBLIGATORIOS PARA MODO OFFLINE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            // Permission categories
            permissionCategories.forEach { category ->
                PermissionCategoryCard(
                    category = category,
                    accentColor = accentRed
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Fixed button at bottom
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color(0xFF0B101D),
            shadowElevation = 16.dp
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentRed
                )
            ) {
                Text(
                    text = "OTORGAR PERMISOS Y CONTINUAR",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = BitchatFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionCategoryCard(
    category: PermissionCategory,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF101726)
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getPermissionIcon(category.type),
                    contentDescription = category.type.nameValue,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = category.type.nameValue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC),
                    fontSize = 13.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

private fun getPermissionIcon(permissionType: PermissionType): ImageVector {
    return when (permissionType) {
        PermissionType.NEARBY_DEVICES -> Icons.Filled.Bluetooth
        PermissionType.PRECISE_LOCATION -> Icons.Filled.LocationOn
        PermissionType.BACKGROUND_LOCATION -> Icons.Filled.LocationOn
        PermissionType.MICROPHONE -> Icons.Filled.Mic
        PermissionType.NOTIFICATIONS -> Icons.Filled.Notifications
        PermissionType.WIFI_AWARE -> Icons.Filled.Wifi
        PermissionType.BATTERY_OPTIMIZATION -> Icons.Filled.Power
        PermissionType.OTHER -> Icons.Filled.Settings
    }
}
