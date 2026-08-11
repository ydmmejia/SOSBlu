package com.bitchat.android.ui

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.mesh.PowerManager
import com.bitchat.android.protocol.LocationSource
import com.bitchat.android.services.BridgeRelayService
import com.bitchat.android.services.EmergencyBeaconService
import com.bitchat.android.services.ReceivedSOSBeacon
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isSOSActive by EmergencyBeaconService.isSOSActive.collectAsState()
    val receivedBeacons by BridgeRelayService.getInstance(context).receivedBeacons.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var freeTextNote by remember { mutableStateOf("") }
    var isDarkMode by remember { mutableStateOf(true) }

    var activePeerCount by remember { mutableIntStateOf(0) }

    // Start BLE mesh services automatically when screen opens & poll active peer count
    LaunchedEffect(Unit) {
        try {
            val mesh = com.bitchat.android.service.MeshServiceHolder.getOrCreate(context)
            mesh.startServices()
        } catch (e: Exception) {
            Log.e("EmergencyScreen", "Error starting mesh service: ${e.message}")
        }

        while (isActive) {
            try {
                val mesh = com.bitchat.android.service.MeshServiceHolder.getOrCreate(context)
                activePeerCount = mesh.getActivePeerCount()
            } catch (_: Exception) { }
            delay(2000L)
        }
    }

    val remainingHours = remember(isSOSActive) {
        PowerManager.getInstance(context).estimateRemainingSOSHours()
    }

    // Colors
    val bgColor = if (isDarkMode) Color(0xFF090D16) else Color(0xFFF8FAFC)
    val topBarBg = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val cardBg = if (isDarkMode) Color(0xFF162032) else Color(0xFFFFFFFF)
    val tabBg = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textMuted = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE53935)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SOSBlu",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = textPrimary
                            )
                            Text(
                                text = "RED DE AUXILIO BLE (OFFLINE)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { isDarkMode = !isDarkMode }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.Brightness4,
                            contentDescription = "Cambiar Tema",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBg
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Bar
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = tabBg,
                contentColor = textPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "EMITIR SEÑAL SOS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (selectedTab == 0) Color(0xFFE53935) else textMuted
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "ALERTAS DE AUXILIO (${receivedBeacons.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (selectedTab == 1) Color(0xFF00E676) else textMuted
                        )
                    }
                )
            }

            if (selectedTab == 0) {
                SpaciousSOSView(
                    context = context,
                    isSOSActive = isSOSActive,
                    activePeerCount = activePeerCount,
                    remainingHours = remainingHours,
                    freeTextNote = freeTextNote,
                    onNoteChange = { freeTextNote = it },
                    isDarkMode = isDarkMode,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textMuted = textMuted
                )
            } else {
                RescueMonitorView(
                    receivedBeacons = receivedBeacons,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textMuted = textMuted
                )
            }
        }
    }
}

@Composable
private fun SpaciousSOSView(
    context: Context,
    isSOSActive: Boolean,
    activePeerCount: Int,
    remainingHours: Float,
    freeTextNote: String,
    onNoteChange: (String) -> Unit,
    isDarkMode: Boolean,
    cardBg: Color,
    textPrimary: Color,
    textMuted: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg)
                .border(1.dp, if (isDarkMode) Color(0xFF25334D) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isSOSActive) Color(0xFFE53935) else Color(0xFF00E676))
                )
                Text(
                    text = if (isSOSActive) "EMITIENDO AUXILIO" else "DISPOSITIVO EN ESPERA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )
            }

            Text(
                text = "$activePeerCount DISPOSITIVOS | %.0fh BATERÍA".format(remainingHours),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textMuted
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center Hero SOS Target
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(220.dp)
                .scale(if (isSOSActive) pulseScale else 1.0f)
                .clip(CircleShape)
                .background(if (isSOSActive) Color(0xFFB71C1C) else Color(0xFFE53935))
                .border(8.dp, if (isSOSActive) Color(0xFFFF5252) else Color(0xFFEF5350), CircleShape)
                .clickable {
                    if (isSOSActive) {
                        EmergencyBeaconService.stopSOS(context)
                    } else {
                        EmergencyBeaconService.startSOS(context, freeTextNote.ifBlank { null })
                    }
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isSOSActive) "SOS EN VIVO" else "PRESIONAR SOS",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isSOSActive) "TOCAR PARA CANCELAR" else "EMITIR ALERTA",
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = freeTextNote,
                onValueChange = onNoteChange,
                label = { Text("Nota de Ubicación Opcional (ej: Piso 3)", color = textMuted, fontSize = 12.sp) },
                singleLine = true,
                enabled = !isSOSActive,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE53935),
                    unfocusedBorderColor = if (isDarkMode) Color(0xFF25334D) else Color(0xFFCBD5E1),
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CellTower,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TRANSMISIÓN CONTINUA EN SEGUNDO PLANO Y PANTALLA BLOQUEADA",
                    color = textMuted,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RescueMonitorView(
    receivedBeacons: List<ReceivedSOSBeacon>,
    cardBg: Color,
    textPrimary: Color,
    textMuted: Color
) {
    if (receivedBeacons.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = null,
                    tint = textMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "NO HAY ALERTAS ACTIVAS EN RANGO",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "El sistema monitorea emisiones de auxilio en la red malla de celular a celular.",
                    color = textMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(receivedBeacons) { beacon ->
                SOSVictimCard(
                    beacon = beacon,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textMuted = textMuted
                )
            }
        }
    }
}

@Composable
private fun SOSVictimCard(
    beacon: ReceivedSOSBeacon,
    cardBg: Color,
    textPrimary: Color,
    textMuted: Color
) {
    val payload = beacon.payload
    val timeAgoMs = (System.currentTimeMillis() - payload.timestamp).coerceAtLeast(0)
    val timeAgoStr = when {
        timeAgoMs < 60_000L -> "hace ${timeAgoMs / 1000}s"
        else -> "hace ${timeAgoMs / 60_000L}m"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ALERTA ID: ${beacon.senderDeviceIdHex.take(8).uppercase()}",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = timeAgoStr,
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Coordenadas GPS de ubicación compartida
            when (payload.locationSource) {
                LocationSource.GPS_LIVE,
                LocationSource.GPS_LAST_KNOWN -> {
                    Text(
                        text = "UBICACIÓN: ${payload.latitude ?: 0.0}, ${payload.longitude ?: 0.0} (±${payload.gpsAccuracy?.toInt() ?: 0}m)",
                        color = textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                LocationSource.NO_GPS_RSSI_ONLY -> {
                    Text(
                        text = "UBICACIÓN: TRANSMISIÓN MESH SIN FIJACIÓN GPS",
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            if (!payload.freeText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "NOTA: ${payload.freeText}",
                    color = textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BATERÍA: ${payload.batteryLevel}% | SALTOS: ${beacon.estimatedHopsPassed}",
                    color = textMuted,
                    fontSize = 10.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0284C7))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "RED MALLA LOCAL",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
