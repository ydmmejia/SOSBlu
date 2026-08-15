package com.bitchat.android.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LocationCheckScreen(
    modifier: Modifier,
    status: LocationStatus,
    onEnableLocation: () -> Unit,
    onRetry: () -> Unit,
    isLoading: Boolean = false
) {
    val bgGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF070B14),
            Color(0xFF0D1424),
            Color(0xFF060912)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            LocationStatus.DISABLED -> {
                LocationDisabledContent(
                    onEnableLocation = onEnableLocation,
                    onRetry = onRetry,
                    isLoading = isLoading
                )
            }
            LocationStatus.NOT_AVAILABLE -> {
                LocationNotAvailableContent()
            }
            LocationStatus.ENABLED -> {
                LocationCheckingContent()
            }
        }
    }
}

@Composable
private fun LocationDisabledContent(
    onEnableLocation: () -> Unit,
    onRetry: () -> Unit,
    isLoading: Boolean
) {
    val accentRed = Color(0xFFE53935)
    val accentRedDark = Color(0xFFB71C1C)

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Icon Badge
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(12.dp, CircleShape, spotColor = accentRed)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(accentRed, accentRedDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ACTIVAR UBICACIÓN GPS",
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = Color(0xFFF8FAFC),
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Indispensable para que los rescatistas sepan dónde estás",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
        }

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0x3338BDF8))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.NearMe,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COORDENADAS ADJUNTAS AL FARO DE RESCATE",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "Cuando presionas SOS, tus coordenadas GPS exactas se empaquetan en el faro binario para guiar a los equipos de búsqueda directamente a tu punto de auxilio.",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 17.sp
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                color = accentRed,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onEnableLocation,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentRed)
                ) {
                    Text(
                        text = "ACTIVAR SERVICIOS DE UBICACIÓN",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Reintentar verificación",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationNotAvailableContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = Color(0xFFE53935),
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "GPS NO DISPONIBLE",
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = Color(0xFFF8FAFC),
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0x33E53935))
        ) {
            Text(
                text = "Este dispositivo no cuenta con sensor de geolocalización GPS. Las alertas se emitirán únicamente por radio de proximidad.",
                fontSize = 12.sp,
                color = Color(0xFFCBD5E1),
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LocationCheckingContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = Color(0xFFE53935),
            modifier = Modifier.size(40.dp)
        )

        Text(
            text = "VERIFICANDO SENSOR GPS...",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.5.sp
        )
    }
}
