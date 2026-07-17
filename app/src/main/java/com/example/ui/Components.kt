package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * NOTE: Icons.Filled.Dialpad lives in the `material-icons-extended` artifact
 * (androidx.compose.material:material-icons-extended). Add that dependency,
 * or swap FloatingDialerButton's icon for a core icon (e.g. Icons.Filled.Apps)
 * if you'd rather not pull in the extended icon set.
 */

// ---- App header: logo wordmark + menu icon, shared by every screen except the active-call screen ----
@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onLogsClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "CYBERGUARD-AI",
            color = TitleBrown,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = "Call Logs",
                tint = Gray800,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onLogsClick)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = Gray800,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onSettingsClick)
            )
        }
    }
}

// ---- Search bar, with an optional trailing "add contact" icon ----
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    showAdd: Boolean = false,
    onAddClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = BorderGray)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search",
            tint = Gray500,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Search name, number.....",
            color = Gray500,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        if (showAdd) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add contact",
                tint = Gray800,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onAddClick)
            )
        }
    }
}

// ---- "Call Logs" / "Call Recordings" tab switcher ----
@Composable
fun TabSwitcher(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        TabButton("Call Logs", selectedTab == 0, Modifier.weight(1f)) { onTabSelected(0) }
        TabButton("Call Recordings", selectedTab == 1, Modifier.weight(1f)) { onTabSelected(1) }
    }
}

@Composable
private fun TabButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(if (selected) BlueBg else Color.White)
            .border(width = 1.dp, color = BorderGray)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Primary else Gray600,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ---- Expanded row action bar: Call / (Add Contact | Edit) / All Call Logs ----
@Composable
fun ActionRow(
    primaryLabel: String,
    primaryIcon: ImageVector,
    onPrimaryClick: () -> Unit,
    secondaryLabel: String,
    secondaryIcon: ImageVector,
    onSecondaryClick: () -> Unit,
    onAllLogsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = Gray200, shape = RoundedCornerShape(8.dp))
    ) {
        ActionCell(primaryLabel, primaryIcon, Modifier.weight(1f), onPrimaryClick)
        VerticalDivider()
        ActionCell(secondaryLabel, secondaryIcon, Modifier.weight(1f), onSecondaryClick)
        VerticalDivider()
        ActionCell("All Call Logs", Icons.AutoMirrored.Filled.List, Modifier.weight(1f), onAllLogsClick)
    }
}

@Composable
private fun ActionCell(
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = SecondaryForeground, modifier = Modifier.size(20.dp))
        Text(text = label, fontSize = 12.sp, color = SecondaryForeground, textAlign = TextAlign.Center)
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(Gray200)
    )
}

// ---- Floating green keypad shortcut, shared by Call Logs / Call Recordings / Contacts ----
@Composable
fun FloatingDialerButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(GreenCall)
            .border(width = 1.dp, color = EmeraldBorder, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Dialpad,
            contentDescription = "Open dialer",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
