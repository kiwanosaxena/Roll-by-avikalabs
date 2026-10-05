package com.avikalabs.roll.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.avikalabs.roll.BuildConfig
import com.avikalabs.roll.R
import com.avikalabs.roll.data.local.ThemeMode
import com.avikalabs.roll.ui.components.GlassBox
import com.avikalabs.roll.ui.theme.AccentSunset
import com.avikalabs.roll.ui.theme.LocalGlassColors
import com.avikalabs.roll.wallpaper.RollLiveWallpaperService
import com.avikalabs.roll.wallpaper.WallpaperApplier

@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    homeDoubleTapEnabled: Boolean,
    onHomeDoubleTapToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glassColors = LocalGlassColors.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Live check of live wallpaper status (§7.6.2)
    var isLiveWallpaperActive by remember { mutableStateOf(checkIsLiveWallpaperActive(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLiveWallpaperActive = checkIsLiveWallpaperActive(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. APPEARANCE SECTION (§7.6.1)
        SectionHeader(title = "Appearance", icon = Icons.Outlined.Palette)

        GlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Theme mode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = glassColors.textPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(glassColors.glassBorder.copy(alpha = 0.25f))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val themes = listOf(
                        ThemeMode.SYSTEM to "System",
                        ThemeMode.DAY to "Day",
                        ThemeMode.NIGHT to "Night"
                    )

                    themes.forEach { (mode, label) ->
                        val isSelected = mode == currentThemeMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) AccentSunset else Color.Transparent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Tab
                                ) { onThemeModeChanged(mode) }
                                .testTag("theme_${mode.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else glassColors.textPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // 2. LIVE WALLPAPER (HOME SCREEN) SECTION (§7.6.2)
        SectionHeader(title = "Live wallpaper (Home screen)", icon = Icons.Outlined.Wallpaper)

        GlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Live Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live wallpaper status",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = glassColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLiveWallpaperActive) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = AccentSunset,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Active",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentSunset
                                )
                            } else {
                                Text(
                                    text = "Not set",
                                    fontSize = 13.sp,
                                    color = glassColors.textSecondary
                                )
                            }
                        }
                    }

                    if (!isLiveWallpaperActive) {
                        Button(
                            onClick = {
                                launchLiveWallpaperPicker(context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentSunset,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_set_live_wallpaper")
                        ) {
                            Text(
                                text = "Set as live wallpaper",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Double Tap to Change Wallpaper Checkbox (§7.6.2)
                val doubleTapEnabledState = homeDoubleTapEnabled && isLiveWallpaperActive
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isLiveWallpaperActive) {
                            onHomeDoubleTapToggle(!homeDoubleTapEnabled)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = doubleTapEnabledState,
                        onCheckedChange = { if (isLiveWallpaperActive) onHomeDoubleTapToggle(it) },
                        enabled = isLiveWallpaperActive,
                        colors = CheckboxDefaults.colors(
                            checkedColor = AccentSunset,
                            uncheckedColor = glassColors.textSecondary,
                            disabledCheckedColor = AccentSunset.copy(alpha = 0.4f),
                            disabledUncheckedColor = glassColors.textSecondary.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("checkbox_double_tap")
                    )
                    Column(modifier = Modifier.padding(start = 6.dp)) {
                        Text(
                            text = "Double tap to change wallpaper",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isLiveWallpaperActive) glassColors.textPrimary else glassColors.textSecondary.copy(alpha = 0.6f)
                        )
                        if (!isLiveWallpaperActive) {
                            Text(
                                text = "Set Roll as your live wallpaper first",
                                fontSize = 11.sp,
                                color = glassColors.textSecondary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // 3. ABOUT SECTION (§7.6.3)
        SectionHeader(title = "About", icon = Icons.Outlined.TouchApp)

        GlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Icon & Name
                Image(
                    painter = painterResource(id = R.drawable.roll_icon_512),
                    contentDescription = "Roll app icon",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Roll",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = glassColors.textPrimary
                )

                // Version from BuildConfig.VERSION_NAME (§7.6.3)
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    fontSize = 12.sp,
                    color = glassColors.textSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "A product of Avikalabs.",
                    fontSize = 12.5.sp,
                    color = glassColors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Contact Row (mailto:avikalabs@gmail.com with ACTION_SENDTO)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(glassColors.glassBorder.copy(alpha = 0.2f))
                        .clickable {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:avikalabs@gmail.com")
                            }
                            try {
                                context.startActivity(emailIntent)
                            } catch (_: Exception) {}
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("contact_support_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = AccentSunset,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "avikalabs@gmail.com",
                        fontSize = 13.5.sp,
                        color = glassColors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = glassColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Privacy Policy Row (§7.6.3 & §14)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(glassColors.glassBorder.copy(alpha = 0.2f))
                        .clickable {
                            val policyIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://avikalabs.com/roll/privacy")
                            }
                            try {
                                context.startActivity(policyIntent)
                            } catch (_: Exception) {}
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("privacy_policy_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PrivacyTip,
                        contentDescription = null,
                        tint = AccentSunset,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Privacy Policy",
                        fontSize = 13.5.sp,
                        color = glassColors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = glassColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    val glassColors = LocalGlassColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentSunset,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = glassColors.textPrimary,
            letterSpacing = 0.5.sp
        )
    }
}

private fun checkIsLiveWallpaperActive(context: Context): Boolean {
    return WallpaperApplier.isLiveWallpaperActive(context)
}

private fun launchLiveWallpaperPicker(context: Context) {
    WallpaperApplier.launchLiveWallpaperPicker(context)
}
