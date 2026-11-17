package org.lingkohijau.app

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            LingkoHijauTheme(darkTheme = isDarkMode) {
                LingkoHijauApp(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

@Composable
fun LingkoHijauApp(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("lingko_prefs", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }

    // Onboarding state
    var isOnboarded by remember { mutableStateOf(prefs.getBoolean("is_onboarded", false)) }
    var userName by remember { mutableStateOf(prefs.getString("user_name", "") ?: "") }
    var userRole by remember {
        val savedRole = prefs.getString("user_role", UserRole.PETUGAS.name) ?: UserRole.PETUGAS.name
        mutableStateOf(UserRole.valueOf(savedRole))
    }

    // Persistent Field Reports (Offline-first local storage)
    val reports = remember {
        val json = prefs.getString("saved_reports", null)
        val loaded: List<FieldReport> = if (json != null) {
            val type = object : TypeToken<List<FieldReport>>() {}.type
            gson.fromJson(json, type)
        } else {
            LingkoRepository.initialReports
        }
        mutableStateListOf<FieldReport>().apply { addAll(loaded) }
    }

    fun saveReportsToStorage() {
        prefs.edit().putString("saved_reports", gson.toJson(reports.toList())).apply()
    }

    if (!isOnboarded) {
        OnboardingScreen(
            initialName = userName,
            initialRole = userRole,
            onComplete = { name, role ->
                userName = name.ifBlank { "Tim Lingko" }
                userRole = role
                isOnboarded = true
                prefs.edit()
                    .putBoolean("is_onboarded", true)
                    .putString("user_name", userName)
                    .putString("user_role", role.name)
                    .apply()
            }
        )
    } else {
        var selectedTab by remember { mutableIntStateOf(0) }
        var showReportDialog by remember { mutableStateOf(false) }
        var preselectedSiteName by remember { mutableStateOf(LingkoRepository.defaultSites.first().name) }

        Scaffold(
            topBar = {
                LingkoTopBar(
                    userName = userName,
                    userRole = userRole,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    val navItems = listOf(
                        Triple("Beranda", Icons.Filled.Home, Icons.Outlined.Home),
                        Triple("Peta Kerja", Icons.Filled.Map, Icons.Outlined.Map),
                        Triple("Laporan", Icons.Filled.Assignment, Icons.Outlined.Assignment),
                        Triple("Kolaborasi", Icons.Filled.Handshake, Icons.Outlined.Handshake)
                    )
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) item.second else item.third,
                                    contentDescription = item.first
                                )
                            },
                            label = { Text(item.first, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showReportDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Filled.AddAPhoto, contentDescription = "Lapor") },
                    text = { Text("Lapor Lapangan", fontWeight = FontWeight.Bold) }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (selectedTab) {
                    0 -> HomeDashboardScreen(
                        userName = userName,
                        userRole = userRole,
                        reports = reports,
                        onOpenMap = { selectedTab = 1 },
                        onOpenReport = { showReportDialog = true }
                    )
                    1 -> MapExplorerScreen(
                        onReportAtSite = { site ->
                            preselectedSiteName = "${site.name} (${site.district})"
                            showReportDialog = true
                        }
                    )
                    2 -> FieldReportsScreen(
                        reports = reports,
                        onSyncAll = {
                            for (i in reports.indices) {
                                reports[i] = reports[i].copy(isSynced = true)
                            }
                            saveReportsToStorage()
                            Toast.makeText(context, "Semua laporan tersinkron ke server Lingko Hijau!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    3 -> CollaborationScreen(
                        userName = userName,
                        userRole = userRole,
                        onResetRole = {
                            isOnboarded = false
                            prefs.edit().putBoolean("is_onboarded", false).apply()
                        }
                    )
                }
            }

            if (showReportDialog) {
                NewFieldReportSheet(
                    defaultSite = preselectedSiteName,
                    reporterName = userName,
                    reporterRole = userRole.title,
                    onDismiss = { showReportDialog = false },
                    onSubmit = { newReport ->
                        reports.add(0, newReport)
                        saveReportsToStorage()
                        showReportDialog = false
                        selectedTab = 2
                        Toast.makeText(context, "Laporan lapangan berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun LingkoLogoMark(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val rOuter = size.minDimension * 0.42f
        val rInner = size.minDimension * 0.18f
        drawCircle(color = tint, radius = rOuter, center = center, style = Stroke(width = 4f))
        drawCircle(color = tint, radius = rInner, center = center, style = Stroke(width = 3.5f))
        // Lodok spider-web radial lines
        drawLine(tint.copy(alpha = 0.75f), Offset(center.x, center.y - rOuter), Offset(center.x, center.y + rOuter), 3f)
        drawLine(tint.copy(alpha = 0.75f), Offset(center.x - rOuter, center.y), Offset(center.x + rOuter, center.y), 3f)
        val diag = rOuter * 0.707f
        drawLine(tint.copy(alpha = 0.75f), Offset(center.x - diag, center.y - diag), Offset(center.x + diag, center.y + diag), 3f)
        drawLine(tint.copy(alpha = 0.75f), Offset(center.x + diag, center.y - diag), Offset(center.x - diag, center.y + diag), 3f)
    }
}

@Composable
fun OnboardingScreen(
    initialName: String,
    initialRole: UserRole,
    onComplete: (String, UserRole) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedRole by remember { mutableStateOf(initialRole) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(EmeraldDark, EmeraldPrimary, TealAccent)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            LingkoLogoMark(modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Lingko Hijau",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Ekosistem Konservasi Flores",
                                color = Color(0xFFA7F3D0),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Filosofi Lodok: Keadilan Ekologis & Kebersamaan",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Pilih peran Anda untuk menyesuaikan alur pelaporan lapangan, pemantauan bibit, dan eksplorasi peta kerja di Manggarai & Flores.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Text(
                "Tahap 1: Identitas & Peran Pengguna",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Lengkap / Organisasi") },
                placeholder = { Text("Contoh: Tri Krismantoro") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) }
            )
        }

        items(UserRole.entries) { role ->
            val isSelected = selectedRole == role
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedRole = role }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = { selectedRole = role })
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(role.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    role.badge,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            role.subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onComplete(name, selectedRole) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Masuk ke Ekosistem Lingko Hijau", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LingkoTopBar(
    userName: String,
    userRole: UserRole,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    LingkoLogoMark(modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Lingko Hijau", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Halo, $userName • ${userRole.title}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onToggleDarkMode) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Tema"
                )
            }
        }
    )
}

@Composable
fun HomeDashboardScreen(
    userName: String,
    userRole: UserRole,
    reports: List<FieldReport>,
    onOpenMap: () -> Unit,
    onOpenReport: () -> Unit
) {
    val extraTrees = reports.sumOf { it.treesCount }
    val totalTrees = 179500 + extraTrees

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
    ) {
        // Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldDark)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            "MANGGARAI & FLORES • REAL-TIME IMPACT",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color(0xFFA7F3D0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Menjaga Hutan Adat & Ketahanan Pangan Lingko",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 25.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Mode aktif: ${userRole.title}. Setiap catatan lapangan langsung memperbarui metrik dampak komunitas.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onOpenMap,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = EmeraldDark
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Peta Flores", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onOpenReport,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+ Kirim Catatan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Real-time Impact Metrics (From Tab 1 & Tab 2 Blueprint)
        item {
            Text("Metrik Dampak Real-Time", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    value = "1.655 Ha",
                    label = "Lahan Lingko",
                    accent = EmeraldPrimary
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    value = "%,d".format(Locale("id", "ID"), totalTrees),
                    label = "Pohon Tertanam",
                    accent = TealAccent
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    value = "22 Desa",
                    label = "Komunitas Adat",
                    accent = EarthAmber
                )
            }
        }

        // 4 Pilar Program Kerja (From Tab 1 & Tab 2 Blueprint)
        item {
            Text("4 Pilar Program Kerja", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        items(LingkoRepository.programs) { prog ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            prog.tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            prog.targetInfo,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(prog.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        prog.summary,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { prog.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(modifier: Modifier = Modifier, value: String, label: String, accent: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = accent)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MapExplorerScreen(
    onReportAtSite: (SiteLocation) -> Unit
) {
    val districts = listOf("Semua", "Ruteng", "Cibal", "Satar Mese", "Reok")
    var selectedDistrict by remember { mutableStateOf("Semua") }
    val filteredSites = remember(selectedDistrict) {
        if (selectedDistrict == "Semua") LingkoRepository.defaultSites
        else LingkoRepository.defaultSites.filter { it.district == selectedDistrict }
    }
    var activeSite by remember { mutableStateOf(LingkoRepository.defaultSites.first()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
    ) {
        item {
            Text("Tahap 2: Eksplorasi Peta Kerja Flores", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Filter wilayah Manggarai & pilih titik pin lokasi untuk melihat capaian konservasi.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // District Filter Chips (Ruteng, Cibal, Satar Mese, Reok)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(districts) { dist ->
                    FilterChip(
                        selected = selectedDistrict == dist,
                        onClick = {
                            selectedDistrict = dist
                            val match = if (dist == "Semua") LingkoRepository.defaultSites.first()
                            else LingkoRepository.defaultSites.firstOrNull { it.district == dist }
                            if (match != null) activeSite = match
                        },
                        label = { Text(dist, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Interactive Custom Map Canvas of Manggarai / Flores
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Topographic contour & Lodok grid overlay
                        val stepX = size.width / 8f
                        val stepY = size.height / 6f
                        for (i in 1..7) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = Offset(stepX * i, 0f),
                                end = Offset(stepX * i, size.height),
                                strokeWidth = 1.5f
                            )
                        }
                        for (j in 1..5) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = Offset(0f, stepY * j),
                                end = Offset(size.width, stepY * j),
                                strokeWidth = 1.5f
                            )
                        }
                        // Stylized Flores Manggarai landmass silhouette
                        drawCircle(
                            color = Color(0xFF10B981).copy(alpha = 0.18f),
                            radius = size.minDimension * 0.65f,
                            center = Offset(size.width * 0.48f, size.height * 0.52f)
                        )
                        drawCircle(
                            color = Color(0xFF34D399).copy(alpha = 0.25f),
                            radius = size.minDimension * 0.42f,
                            center = Offset(size.width * 0.45f, size.height * 0.48f),
                            style = Stroke(
                                width = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                            )
                        )
                    }

                    Text(
                        "KABUPATEN MANGGARAI — FLORES, NTT",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp),
                        color = Color(0xFFA7F3D0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Interactive Pins
                    filteredSites.forEach { site ->
                        val isSelected = activeSite.id == site.id
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(
                                        x = (260 * site.mapXFraction).dp,
                                        y = (155 * site.mapYFraction).dp
                                    )
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) Color(0xFFF59E0B) else Color(0xFF10B981))
                                    .border(2.dp, Color.White, RoundedCornerShape(50))
                                    .clickable { activeSite = site }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "● ${site.district}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Site Detail Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                activeSite.status,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            "GPS: ${activeSite.coordinates}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "${activeSite.name} (${activeSite.district})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        activeSite.program,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        activeSite.description,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Luas Dampingan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${activeSite.areaHa} Hektar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Bibit Tertanam", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "%,d Pohon".format(Locale("id", "ID"), activeSite.treesPlanted),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onReportAtSite(activeSite) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kirim Laporan Lapangan di ${activeSite.district}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FieldReportsScreen(
    reports: List<FieldReport>,
    onSyncAll: () -> Unit
) {
    val unsyncedCount = reports.count { !it.isSynced }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tahap 3 & 4: Pelaporan & Sinkronisasi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "${reports.size} catatan lapangan tersimpan lokal",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(onClick = onSyncAll, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (unsyncedCount > 0) "Sync ($unsyncedCount)" else "Tersinkron", fontSize = 12.sp)
                }
            }
        }

        items(reports) { report ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                report.category,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (report.hasPhoto) {
                                Icon(
                                    Icons.Filled.PhotoCamera,
                                    contentDescription = "Foto terlampir",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                report.timestamp,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(report.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "📍 ${report.siteName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        report.notes,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Oleh: ${report.reporterName} (${report.reporterRole})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (report.treesCount > 0) {
                            Text(
                                "+${report.treesCount} Bibit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewFieldReportSheet(
    defaultSite: String,
    reporterName: String,
    reporterRole: String,
    onDismiss: () -> Unit,
    onSubmit: (FieldReport) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedSite by remember { mutableStateOf(defaultSite) }
    var category by remember { mutableStateOf("Agroforestri") }
    var notes by remember { mutableStateOf("") }
    var treesText by remember { mutableStateOf("50") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membuka kamera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin kamera ditolak. Silakan pilih foto dari galeri.", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                capturedBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat gambar dari galeri", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text("Kirim Catatan & Foto Lapangan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Mendukung mode offline-first — data tersimpan di perangkat & siap sinkron.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Kegiatan / Temuan *") },
                    placeholder = { Text("Contoh: Penanaman Bibit Mahoni & Kopi") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Text("Pilih Lokasi Kerja", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LingkoRepository.defaultSites) { site ->
                        val fullLabel = "${site.name} (${site.district})"
                        FilterChip(
                            selected = selectedSite == fullLabel,
                            onClick = { selectedSite = fullLabel },
                            label = { Text(site.district, fontSize = 12.sp) }
                        )
                    }
                }
            }

            item {
                val categories = listOf("Agroforestri", "Konservasi Hutan", "Energi & Edukasi", "Masyarakat Adat")
                Text("Kategori Program", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = treesText,
                    onValueChange = { treesText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Jumlah Bibit / Pohon Terdata") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Kondisi Lapangan *") },
                    placeholder = { Text("Tuliskan kondisi tanah, cuaca, atau kebutuhan komunitas...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kamera", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galeri", fontSize = 12.sp)
                    }
                }

                capturedBitmap?.let { bmp ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Preview Foto",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        if (title.isBlank() || notes.isBlank()) {
                            Toast.makeText(context, "Harap isi judul dan catatan lapangan", Toast.LENGTH_SHORT).show()
                        } else {
                            val timeStr = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")).format(Date())
                            onSubmit(
                                FieldReport(
                                    id = "RPT-${System.currentTimeMillis() % 10000}",
                                    title = title.trim(),
                                    siteName = selectedSite,
                                    category = category,
                                    notes = notes.trim(),
                                    reporterName = reporterName,
                                    reporterRole = reporterRole,
                                    timestamp = "$timeStr WITA",
                                    treesCount = treesText.toIntOrNull() ?: 0,
                                    hasPhoto = capturedBitmap != null,
                                    isSynced = false
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simpan Laporan Lapangan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CollaborationScreen(
    userName: String,
    userRole: UserRole,
    onResetRole: () -> Unit
) {
    val context = LocalContext.current
    var partnerOrg by remember { mutableStateOf("") }
    var partnerEmail by remember { mutableStateOf("") }
    var collabTopic by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
    ) {
        item {
            Text("Kolaborasi & Tata Kelola Lingko Hijau", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Ajukan kemitraan program konservasi, dukungan donor, atau jaringan relawan Flores.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Formulir Pengajuan Kemitraan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    OutlinedTextField(
                        value = partnerOrg,
                        onValueChange = { partnerOrg = it },
                        label = { Text("Nama Lembaga / Komunitas") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = partnerEmail,
                        onValueChange = { partnerEmail = it },
                        label = { Text("Email / WhatsApp Kontak") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = collabTopic,
                        onValueChange = { collabTopic = it },
                        label = { Text("Bentuk Dukungan / Kolaborasi") },
                        placeholder = { Text("Contoh: Adopsi 1.000 bibit agroforestri Ruteng") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = {
                            if (partnerOrg.isBlank() || collabTopic.isBlank()) {
                                Toast.makeText(context, "Lengkapi nama lembaga & rencana kolaborasi", Toast.LENGTH_SHORT).show()
                            } else {
                                partnerOrg = ""
                                partnerEmail = ""
                                collabTopic = ""
                                Toast.makeText(context, "Proposal kolaborasi terkirim ke Sekretariat Lingko Hijau!", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Kirim Pengajuan Kolaborasi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Profil Sesi Aktif", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Pengguna: $userName", fontSize = 13.sp)
                    Text("Peran: ${userRole.title} (${userRole.badge})", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(onClick = onResetRole, shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Filled.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ganti Peran Pengguna", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
