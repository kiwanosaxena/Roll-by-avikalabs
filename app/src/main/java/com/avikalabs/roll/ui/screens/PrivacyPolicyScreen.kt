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

import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration

private const val ONLINE_PRIVACY_URL = "https://kiwanosaxena.github.io/Roll-by-avikalabs/privacy"

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

                    lines.forEach { line ->
                        val trimmed = line.trim()
                        when {
                            trimmed.isEmpty() -> {
                                Spacer(modifier = Modifier.height(8.dp))
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
                            trimmed.startsWith("# ") -> {
                                val title = trimmed.removePrefix("# ").trim()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = glassColors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            trimmed.startsWith("## ") -> {
                                val section = trimmed.removePrefix("## ").trim()
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = section,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = glassColors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            trimmed.startsWith("- ") -> {
                                val bulletText = trimmed.removePrefix("- ").trim()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.5.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "•",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentSunset,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    RenderFormattedText(
                                        text = bulletText,
                                        textColor = glassColors.textSecondary,
                                        linkColor = AccentSunset,
                                        onLinkClick = { url ->
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            } catch (_: Exception) {}
                                        }
                                    )
                                }
                            }
                            else -> {
                                RenderFormattedText(
                                    text = trimmed,
                                    textColor = glassColors.textSecondary,
                                    linkColor = AccentSunset,
                                    modifier = Modifier.padding(vertical = 2.5.dp),
                                    onLinkClick = { url ->
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        } catch (_: Exception) {}
                                    }
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

/**
 * Parses inline markdown:
 * - Text wrapped in **bold** -> shown in bold without asterisks
 * - [label](url) links -> shown in linkColor with underline and clickable
 */
@Composable
private fun RenderFormattedText(
    text: String,
    textColor: androidx.compose.ui.graphics.Color,
    linkColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit
) {
    val annotatedString = buildAnnotatedString {
        // Regex matches [link text](url) or **bold text**
        val regex = Regex("""\[([^\]]+)\]\(([^)]+)\)|\*\*([^*]+)\*\*""")
        var currentIndex = 0

        for (match in regex.findAll(text)) {
            val range = match.range
            if (range.first > currentIndex) {
                append(text.substring(currentIndex, range.first))
            }

            val linkLabel = match.groups[1]?.value
            val linkUrl = match.groups[2]?.value
            val boldContent = match.groups[3]?.value

            if (linkLabel != null && linkUrl != null) {
                val start = length
                append(linkLabel)
                val end = length
                addStyle(
                    style = SpanStyle(
                        color = linkColor,
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = start,
                    end = end
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = linkUrl,
                    start = start,
                    end = end
                )
            } else if (boldContent != null) {
                val start = length
                append(boldContent)
                val end = length
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = start,
                    end = end
                )
            }

            currentIndex = range.last + 1
        }

        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }

    @Suppress("DEPRECATION")
    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = androidx.compose.ui.text.TextStyle(
            color = textColor,
            fontSize = 13.5.sp,
            lineHeight = 20.sp
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onLinkClick(annotation.item)
                }
        }
    )
}
