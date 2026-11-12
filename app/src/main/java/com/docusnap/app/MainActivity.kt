package com.docusnap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF101827)
private val Panel = Color(0xFF192538)
private val Accent = Color(0xFF8BC5FF)
private val Muted = Color(0xFF9BAAC0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply the first Snapdoc design theme.
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Ink, surface = Panel, primary = Accent,
                onPrimary = Ink, onBackground = Color.White, onSurface = Color.White
            )) { SnapdocApp() }
        }
    }
}

@Composable
private fun SnapdocApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    Scaffold(containerColor = Ink, bottomBar = {
        NavigationBar(containerColor = Panel) {
            NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                icon = { Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Home") }, label = { Text("Home") })
            NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                icon = { Icon(Icons.Outlined.FolderOpen, contentDescription = "Files") }, label = { Text("Files") })
            NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }, label = { Text("Settings") })
        }
    }) { padding ->
        when (selectedTab) {
            0 -> HomeScreen(Modifier.padding(padding))
            1 -> PlaceholderScreen("Your documents", "Your saved scans and PDFs will appear here.", Modifier.padding(padding))
            else -> PlaceholderScreen("Settings", "Privacy, export and app preferences.", Modifier.padding(padding))
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Spacer(Modifier.height(18.dp))
            Text("SNAPDOC", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Documents,\nmade simple.", fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
            Text("Scan and manage files privately on your device.", color = Muted, fontSize = 14.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF233A55)),
                shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background(Accent.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = Accent)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("ON-DEVICE", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Turn paper into\nbeautiful PDFs", fontSize = 23.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
                    Text("Your documents stay on this phone. No account required.", color = Muted, fontSize = 13.sp)
                    Button(onClick = { /* Scanner feature will be wired in Phase 2. */ }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                        Text("  Start scanning")
                    }
                }
            }
        }
        item {
            Text("Quick tools", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolTile("Images to PDF", Icons.Outlined.Image, Modifier.weight(1f))
                ToolTile("Merge PDFs", Icons.Outlined.Merge, Modifier.weight(1f))
                ToolTile("PDF tools", Icons.Outlined.PictureAsPdf, Modifier.weight(1f))
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent documents", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("See all", color = Accent, fontSize = 13.sp)
            }
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = Muted)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("No documents yet", fontWeight = FontWeight.SemiBold)
                        Text("Your scans will be saved here.", color = Muted, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ToolTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(112.dp).clickable { /* Tool route is added with the PDF feature. */ },
        colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(23.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, message: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize(), color = Ink) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(message, color = Muted)
        }
    }
}