package com.avikalabs.roll.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avikalabs.roll.R
import com.avikalabs.roll.ui.components.GlassBox
import com.avikalabs.roll.ui.theme.AccentSunset
import com.avikalabs.roll.ui.theme.LocalGlassColors

private const val ONLINE_PRIVACY_URL = "https://kiwanosaxena.github.io/Roll-by-avikalabs/privacy"

private val KNOWN_HEADINGS = setOf(
    "Privacy Policy for Roll",
    "The short version",
    "Information we collect",
    "Your photos and folders",
    "Information stored on your device",
    "Device backups",
    "Permissions Roll uses",
    "Third-party services",
    "Children’s privacy",
    "Children's privacy",
    "Children\'s privacy",
    "Security",
    "Changes to this policy",
    "Contact us"
)

@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalGlassColors.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val fullPolicyText = stringResource(R.string.privacy_policy_body)

    BackHandler { onNavigateBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("privacy_policy_screen")
    ) {
        // Top bar with back arrow and title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .testTag("btn_back_privacy")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = glassColors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Privacy Policy",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = glassColors.textPrimary
            )
        }

        // Scrollable content in the app's glass style
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            GlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevated = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    val lines = fullPolicyText.split("\n")
                    var inListSection = false

                    lines.forEach { line ->
                        val trimmed = line.trim()
                        when {
                            trimmed.isEmpty() -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                inListSection = false
                            }
                            trimmed in KNOWN_HEADINGS -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = trimmed,
                                    fontSize = if (trimmed == "Privacy Policy for Roll") 18.sp else 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = glassColors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                inListSection = false
                            }
                            trimmed.startsWith("Effective date:") -> {
                                Text(
                                    text = trimmed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AccentSunset,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            trimmed.startsWith("Set wallpaper:") ||
                            trimmed.startsWith("Run at startup:") ||
                            trimmed.startsWith("Photo and folder access:") ||
                            trimmed.startsWith("the list of") ||
                            trimmed.startsWith("your timer") ||
                            trimmed.startsWith("app preferences") ||
                            trimmed.startsWith("Roll does not upload") ||
                            trimmed.startsWith("Roll keeps permission") ||
                            trimmed.startsWith("If you add a folder") ||
                            trimmed.startsWith("You can remove any") -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "•",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentSunset,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = trimmed,
                                        fontSize = 13.5.sp,
                                        lineHeight = 20.sp,
                                        color = glassColors.textSecondary
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = trimmed,
                                    fontSize = 13.5.sp,
                                    lineHeight = 20.sp,
                                    color = glassColors.textSecondary,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Small "View online" link at bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ONLINE_PRIVACY_URL))
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .padding(vertical = 10.dp)
                            .testTag("btn_view_online"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.action_view_online),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentSunset
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(R.string.action_view_online),
                            tint = AccentSunset,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
