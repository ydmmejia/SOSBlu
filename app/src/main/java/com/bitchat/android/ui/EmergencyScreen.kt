package com.bitchat.android.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
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

    // Unified Neutral Dark / Light Palette
    val bgGradient = if (isDarkMode) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF070B14),
                Color(0xFF0D1424),
                Color(0xFF060912)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFF8FAFC),
                Color(0xFFEDF2F7),
                Color(0xFFF1F5F9)
            )
        )
    }

    val topBarBg = if (isDarkMode) Color(0xFF0B101D) else Color(0xFFFFFFFF)
    val cardBg = if (isDarkMode) Color(0xFF131B2E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDarkMode) Color(0x3338BDF8) else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accentRed = Color(0xFFE53935)
    val accentRedDark = Color(0xFFB71C1C)
    val accentGreen = Color(0xFF00E676)
    val accentBlue = Color(0xFF38BDF8)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Glowing Brand Shield
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .shadow(8.dp, RoundedCornerShape(10.dp), spotColor = accentRed)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(accentRed, accentRedDark)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SOSBlu",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    letterSpacing = (-0.5).sp,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accentRed.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "OFFLINE",
                                        color = accentRed,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                            Text(
                                text = "SISTEMA DE AUXILIO EN RED MALLA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isDarkMode = !isDarkMode },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.Brightness4,
                            contentDescription = "Cambiar tema",
                            tint = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBg
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Floating Segmented Pill Controller
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDarkMode) Color(0xFF101726) else Color(0xFFE2E8F0))
                            .border(1.dp, if (isDarkMode) Color(0xFF1E293B) else Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                            .padding(4.dp)
                    ) {
                        // Tab 0: Emitir Auxilio
                        val tab0Modifier = if (selectedTab == 0) {
                            Modifier.background(Brush.linearGradient(listOf(Color(0xFFE53935), Color(0xFFC62828))))
                        } else {
                            Modifier.background(Color.Transparent)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .then(tab0Modifier)
                                .clickable { selectedTab = 0 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) Color.White else textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "EMITIR ALERTA",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = if (selectedTab == 0) Color.White else textSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // Tab 1: Alertas Recibidas (Deduplicated Count)
                        val tab1Modifier = if (selectedTab == 1) {
                            Modifier.background(Brush.linearGradient(listOf(Color(0xFF00C853), Color(0xFF009624))))
                        } else {
                            Modifier.background(Color.Transparent)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .then(tab1Modifier)
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = if (selectedTab == 1) Color.White else textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ALERTAS (${receivedBeacons.size})",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = if (selectedTab == 1) Color.White else textSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                if (selectedTab == 0) {
                    PremiumSOSView(
                        context = context,
                        isSOSActive = isSOSActive,
                        activePeerCount = activePeerCount,
                        remainingHours = remainingHours,
                        freeTextNote = freeTextNote,
                        onNoteChange = { freeTextNote = it },
                        isDarkMode = isDarkMode,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentRed = accentRed,
                        accentGreen = accentGreen,
                        accentBlue = accentBlue
                    )
                } else {
                    PremiumRescueMonitorView(
                        receivedBeacons = receivedBeacons,
                        isDarkMode = isDarkMode,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentRed = accentRed,
                        accentGreen = accentGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumSOSView(
    context: Context,
    isSOSActive: Boolean,
    activePeerCount: Int,
    remainingHours: Float,
    freeTextNote: String,
    onNoteChange: (String) -> Unit,
    isDarkMode: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentRed: Color,
    accentGreen: Color,
    accentBlue: Color
) {
    val scrollState = rememberScrollState()

    // Pulse animations for radar rings
    val infiniteTransition = rememberInfiniteTransition(label = "hero_radar")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )

    // Active broadcast timer
    var broadcastSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isSOSActive) {
        if (isSOSActive) {
            broadcastSeconds = 0L
            while (isActive && isSOSActive) {
                delay(1000L)
                broadcastSeconds += 1
            }
        }
    }

    // Dynamic, responsive layout with imePadding and vertical scrolling for all screen sizes
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth

        // Scale button proportionally based on available screen space
        val buttonSize = min(
            min(screenWidth * 0.52f, screenHeight * 0.28f),
            200.dp
        ).coerceAtLeast(130.dp)

        val outerPulseSize = buttonSize + 30.dp
        val middleRingSize = buttonSize + 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Telemetry HUD Grid (3 Metric Cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Red Malla Card
                TelemetryHUDCard(
                    modifier = Modifier.weight(1f),
                    title = "RED MALLA",
                    value = "$activePeerCount NODOS",
                    subtitle = if (activePeerCount > 0) "Enlazado" else "Buscando",
                    indicatorColor = if (activePeerCount > 0) accentGreen else accentBlue,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                // 2. Estado SOS Card
                TelemetryHUDCard(
                    modifier = Modifier.weight(1f),
                    title = "ESTADO FARO",
                    value = if (isSOSActive) "EMITIENDO" else "EN ESPERA",
                    subtitle = if (isSOSActive) "%02d:%02d".format(broadcastSeconds / 60, broadcastSeconds % 60) else "Listo",
                    indicatorColor = if (isSOSActive) accentRed else accentGreen,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                // 3. Autonomía Card
                TelemetryHUDCard(
                    modifier = Modifier.weight(1f),
                    title = "AUTONOMÍA",
                    value = "%.0fh".format(remainingHours),
                    subtitle = "Batería SOS",
                    indicatorColor = Color(0xFFFBBF24),
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Responsive Center Hero SOS Button with Concentric Rings
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(outerPulseSize + 20.dp)
            ) {
                // Layer 1: Outer Pulse Wave
                Box(
                    modifier = Modifier
                        .size(outerPulseSize)
                        .scale(if (isSOSActive) pulseScale1 else 1.05f)
                        .alpha(if (isSOSActive) pulseAlpha1 else 0.15f)
                        .clip(CircleShape)
                        .background(if (isSOSActive) accentRed else accentBlue)
                )

                // Layer 2: Middle Ring
                Box(
                    modifier = Modifier
                        .size(middleRingSize)
                        .scale(if (isSOSActive) pulseScale2 else 1.0f)
                        .clip(CircleShape)
                        .background(if (isSOSActive) Color(0x33E53935) else Color(0x1538BDF8))
                        .border(
                            BorderStroke(
                                2.dp,
                                if (isSOSActive) Brush.radialGradient(listOf(Color(0xFFFF8A80), Color(0xFFD50000)))
                                else Brush.radialGradient(listOf(Color(0x6638BDF8), Color(0x221E293B)))
                            ),
                            CircleShape
                        )
                )

                // Layer 3: Main Tactical SOS Action Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(buttonSize)
                        .shadow(
                            elevation = if (isSOSActive) 28.dp else 12.dp,
                            shape = CircleShape,
                            spotColor = if (isSOSActive) accentRed else Color.Black
                        )
                        .clip(CircleShape)
                        .background(
                            if (isSOSActive) {
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFF5252),
                                        Color(0xFFD32F2F),
                                        Color(0xFF8B0000)
                                    )
                                )
                            } else {
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFEF5350),
                                        Color(0xFFE53935),
                                        Color(0xFFB71C1C)
                                    )
                                )
                            }
                        )
                        .border(
                            BorderStroke(
                                3.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color(0x99FFFFFF),
                                        Color(0x22FFFFFF),
                                        Color(0x66FFFFFF)
                                    )
                                )
                            ),
                            CircleShape
                        )
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
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(if (buttonSize < 160.dp) 36.dp else 46.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isSOSActive) "SOS EN VIVO" else "PRESIONAR SOS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = if (buttonSize < 160.dp) 14.sp else 16.sp,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSOSActive) "TOCAR PARA DETENER" else "TRANSMITIR AUXILIO",
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold,
                            fontSize = if (buttonSize < 160.dp) 9.sp else 10.sp,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Location Detail Input Field
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = freeTextNote,
                    onValueChange = onNoteChange,
                    label = { Text("Detalle de ubicación (ej: Piso 2, bajo losa)", color = textSecondary, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.EditLocation,
                            contentDescription = null,
                            tint = if (freeTextNote.isNotBlank()) accentRed else textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    enabled = !isSOSActive,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentRed,
                        unfocusedBorderColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1),
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Transmission Reassurance Banner
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkMode) Color(0x2238BDF8) else Color(0x1538BDF8))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = null,
                        tint = accentGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TRANSMISIÓN CONTINUA EN SEGUNDO PLANO Y PANTALLA BLOQUEADA",
                        color = textSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryHUDCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    indicatorColor: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    color = textSecondary,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
            }
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PremiumRescueMonitorView(
    receivedBeacons: List<ReceivedSOSBeacon>,
    isDarkMode: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentRed: Color,
    accentGreen: Color
) {
    if (receivedBeacons.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(if (isDarkMode) Color(0xFF131B2E) else Color(0xFFE2E8F0))
                        .border(1.dp, if (isDarkMode) Color(0xFF1E293B) else Color(0xFFCBD5E1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "MONITOREANDO RED DE AUXILIO",
                    color = textPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Escaneando continuamente señales de auxilio emitidas por dispositivos cercanos en modo offline.",
                    color = textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(receivedBeacons, key = { it.senderDeviceIdHex }) { beacon ->
                PremiumVictimCard(
                    beacon = beacon,
                    isDarkMode = isDarkMode,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accentRed = accentRed,
                    accentGreen = accentGreen
                )
            }
        }
    }
}

@Composable
private fun PremiumVictimCard(
    beacon: ReceivedSOSBeacon,
    isDarkMode: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentRed: Color,
    accentGreen: Color
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val payload = beacon.payload
    val timeAgoMs = (System.currentTimeMillis() - beacon.receivedTimestamp).coerceAtLeast(0)
    val timeAgoStr = when {
        timeAgoMs < 60_000L -> "hace ${timeAgoMs / 1000}s"
        else -> "hace ${timeAgoMs / 60_000L}m"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: ID + Time Ago + Signal Retransmission Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = accentRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "ALERTA #${beacon.senderDeviceIdHex.take(8).uppercase()}",
                            color = textPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(accentGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Señal de auxilio activa",
                                color = accentGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = timeAgoStr,
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }
            }

            // GPS Location Card
            when (payload.locationSource) {
                LocationSource.GPS_LIVE,
                LocationSource.GPS_LAST_KNOWN -> {
                    val lat = payload.latitude ?: 0.0
                    val lon = payload.longitude ?: 0.0
                    val acc = payload.gpsAccuracy?.toInt() ?: 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                            .border(1.dp, if (isDarkMode) Color(0xFF1E293B) else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = accentRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "UBICACIÓN GPS (±${acc}m)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "%.6f, %.6f".format(lat, lon),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = textPrimary
                            )
                        }

                        // Open in Map Button
                        IconButton(
                            onClick = {
                                val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(Alerta+SOSBlu)")
                                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                try {
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    clipboardManager.setText(AnnotatedString("$lat,$lon"))
                                    Toast.makeText(context, "Coordenadas copiadas al portapapeles", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accentRed.copy(alpha = 0.15f))
                                .size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Ver en mapa",
                                tint = accentRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                LocationSource.NO_GPS_RSSI_ONLY -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "UBICACIÓN: TRANSMISIÓN MALLA SIN COORDENADAS GPS",
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            // Location Detail Callout (if present)
            if (!payload.freeText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF38BDF8).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "DETALLE:",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = payload.freeText,
                            color = textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Footer: Battery + Hops + Local Mesh Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = if (payload.batteryLevel > 20) accentGreen else accentRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${payload.batteryLevel}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )
                    }

                    Text(
                        text = "•",
                        color = textSecondary,
                        fontSize = 10.sp
                    )

                    Text(
                        text = "${beacon.estimatedHopsPassed} SALTO(S)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0284C7))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "RED MALLA LOCAL",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}
