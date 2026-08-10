package com.bitchat.android.ui

import android.content.Context
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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.bitchat.android.services.GatewayRelayStatus
import com.bitchat.android.services.ReceivedSOSBeacon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isSOSActive by EmergencyBeaconService.isSOSActive.collectAsState()
    val currentPayload by EmergencyBeaconService.currentPayload.collectAsState()
    val receivedBeacons by BridgeRelayService.getInstance(context).receivedBeacons.collectAsState()
    val isGatewayConnected by BridgeRelayService.getInstance(context).isGatewayConnected.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var freeTextNote by remember { mutableStateOf("") }

    val activePeerCount = remember {
        try {
            com.bitchat.android.service.MeshServiceHolder.getOrCreate(context).getActivePeerCount()
        } catch (e: Exception) { 0 }
    }

    val remainingHours = remember(isSOSActive) {
        PowerManager.getInstance(context).estimateRemainingSOSHours()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SOSBlu",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            text = "RED DE AUXILIO BLE - OFFLINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5252)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Chat Auxiliar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF141414)
                )
            )
        },
        bottomBar = {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF262626),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ABRIR CHAT AUXILIAR DE MALLA",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1A1A1A),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "PEDIR AUXILIO (SOS)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 0) Color(0xFFFF5252) else Color.Gray
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "SEÑALES RECIBIDAS (${receivedBeacons.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 1) Color(0xFFFFB74D) else Color.Gray
                        )
                    }
                )
            }

            if (selectedTab == 0) {
                VictimTransmitterView(
                    context = context,
                    isSOSActive = isSOSActive,
                    activePeerCount = activePeerCount,
                    remainingHours = remainingHours,
                    freeTextNote = freeTextNote,
                    onNoteChange = { freeTextNote = it },
                    isGatewayConnected = isGatewayConnected
                )
            } else {
                RescueMonitorView(
                    receivedBeacons = receivedBeacons,
                    isGatewayConnected = isGatewayConnected
                )
            }
        }
    }
}

@Composable
private fun VictimTransmitterView(
    context: Context,
    isSOSActive: Boolean,
    activePeerCount: Int,
    remainingHours: Float,
    freeTextNote: String,
    onNoteChange: (String) -> Unit,
    isGatewayConnected: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Tarjeta Telemática de Estado
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ESTADO DE TRANSMISIÓN",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSOSActive) Color(0xFFD32F2F) else Color(0xFF333333))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isSOSActive) "SOS ACTIVO (TRANSMITIENDO)" else "EN ESPERA",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "EQUIPOS CERCANOS", color = Color.Gray, fontSize = 11.sp)
                        Text(text = "$activePeerCount DIRECTOS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column {
                        Text(text = "AUTONOMÍA ESTIMADA", color = Color.Gray, fontSize = 11.sp)
                        Text(text = "%.1f HORAS".format(remainingHours), color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = null,
                        tint = if (isGatewayConnected) Color(0xFF66BB6A) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isGatewayConnected) "CONECTADO A CENTRAL DE RESCATE (INTERNET OK)" else "RED MALLA LOCAL SIN INTERNET (RETRANSMITIENDO)",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // BOTÓN GIGANTE SOS (UN SOLO TOQUE)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(230.dp)
                .clip(CircleShape)
                .background(if (isSOSActive) Color(0xFFB71C1C) else Color(0xFFD32F2F))
                .border(8.dp, if (isSOSActive) Color(0xFFFF5252) else Color(0xFFEF5350), CircleShape)
                .clickable {
                    if (isSOSActive) {
                        EmergencyBeaconService.stopSOS(context)
                    } else {
                        EmergencyBeaconService.startSOS(context, freeTextNote.ifBlank { null })
                    }
                }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isSOSActive) "DETENER SOS" else "PRESIONAR\nSOS AUXILIO",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.2.sp
                )
            }
        }

        // Campo de Nota de Ubicación Opcional
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = freeTextNote,
                onValueChange = onNoteChange,
                label = { Text("Nota de Ubicación Opcional (ej: Piso 3, Apto 302)", color = Color.Gray) },
                singleLine = true,
                enabled = !isSOSActive,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFD32F2F),
                    unfocusedBorderColor = Color(0xFF424242),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "OPERACIÓN DE UN SOLO TOQUE. TRANSMITE CONTINUAMENTE EN SEGUNDO PLANO Y PANTALLA BLOQUEADA.",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RescueMonitorView(
    receivedBeacons: List<ReceivedSOSBeacon>,
    isGatewayConnected: Boolean
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
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "NO HAY SEÑALES DE AUXILIO EN RANGO",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Escuchando emisiones BLE de emergencia emitidas por víctimas cercanas en la malla.",
                    color = Color.Gray,
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
                SOSBeaconCard(beacon = beacon)
            }
        }
    }
}

@Composable
private fun SOSBeaconCard(beacon: ReceivedSOSBeacon) {
    val payload = beacon.payload
    val timeAgoMs = (System.currentTimeMillis() - payload.timestamp).coerceAtLeast(0)
    val timeAgoStr = when {
        timeAgoMs < 60_000L -> "hace ${timeAgoMs / 1000}s"
        else -> "hace ${timeAgoMs / 60_000L}m"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
        shape = RoundedCornerShape(10.dp)
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
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VÍCTIMA ID: ${beacon.senderDeviceIdHex.take(8).uppercase()}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = timeAgoStr,
                    color = Color(0xFFFFB74D),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Detalles de Ubicación
            when (payload.locationSource) {
                LocationSource.GPS_LIVE,
                LocationSource.GPS_LAST_KNOWN -> {
                    Text(
                        text = "COORDENADAS GPS: ${payload.latitude ?: 0.0}, ${payload.longitude ?: 0.0} (±${payload.gpsAccuracy?.toInt() ?: 0}m)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                LocationSource.NO_GPS_RSSI_ONLY -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF262626))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "SIN GPS - TRIANGULAR POR INTENSIDAD RSSI (${beacon.rssi ?: "N/A"} dBm)",
                            color = Color(0xFFFFB74D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (!payload.freeText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "NOTA DE VÍCTIMA: ${payload.freeText}",
                    color = Color(0xFFE0E0E0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BATERÍA: ${payload.batteryLevel}% | HOPS: ${beacon.estimatedHopsPassed}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (beacon.relayStatus == GatewayRelayStatus.RELAYED_TO_GATEWAY)
                                Color(0xFF2E7D32)
                            else
                                Color(0xFFE65100)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (beacon.relayStatus == GatewayRelayStatus.RELAYED_TO_GATEWAY)
                            "ENVIADO A CENTRAL"
                        else
                            "RETRANSMISIÓN MALLA LOCAL",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
