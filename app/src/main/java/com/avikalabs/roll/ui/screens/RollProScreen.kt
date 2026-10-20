package com.avikalabs.roll.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avikalabs.roll.R
import com.avikalabs.roll.ui.components.GlassBox
import com.avikalabs.roll.ui.components.GlassButton
import com.avikalabs.roll.ui.theme.AccentSunset
import com.avikalabs.roll.ui.theme.LocalGlassColors

@Composable
fun RollProScreen(
    isPro: Boolean,
    formattedPrice: String?,
    isBillingAvailable: Boolean,
    statusMessage: String?,
    onUnlockClick: () -> Unit,
    onRestorePurchasesClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalGlassColors.current

    val proFeatures = listOf(
        stringResource(R.string.pro_feature_photos),
        stringResource(R.string.pro_feature_lock_roll),
        stringResource(R.string.pro_feature_shuffle),
        stringResource(R.string.pro_feature_folder_sync),
        stringResource(R.string.pro_feature_unlock_trigger),
        stringResource(R.string.pro_feature_double_tap),
        stringResource(R.string.pro_feature_widget_advance)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row with Title and Close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AccentSunset.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = AccentSunset,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.pro_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = glassColors.textPrimary
                )
            }

            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .testTag("btn_close_pro")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.pro_close_content_description),
                    tint = glassColors.textPrimary
                )
            }
        }

        // Status or notification message if present
        if (!statusMessage.isNullOrEmpty()) {
            GlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = statusMessage,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentSunset,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }
        }

        if (!isBillingAvailable && !isPro) {
            GlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.pro_billing_unavailable),
                    fontSize = 13.sp,
                    color = glassColors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }
        }

        // Feature list container
        GlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(16.dp),
            elevated = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                proFeatures.forEachIndexed { index, feature ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(AccentSunset.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = AccentSunset,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = feature,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = glassColors.textPrimary
                        )
                    }
                }
            }
        }

        // Main action button
        if (isPro) {
            GlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LockOpen,
                        contentDescription = null,
                        tint = AccentSunset,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.pro_unlocked_thank_you),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentSunset
                    )
                }
            }
        } else {
            val unlockButtonText = if (!formattedPrice.isNullOrEmpty()) {
                stringResource(R.string.pro_unlock_button_format, formattedPrice)
            } else {
                stringResource(R.string.pro_unlock_button_default)
            }

            Button(
                onClick = onUnlockClick,
                enabled = isBillingAvailable,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentSunset,
                    contentColor = Color.White,
                    disabledContainerColor = AccentSunset.copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_unlock_pro")
            ) {
                Text(
                    text = unlockButtonText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle
            Text(
                text = stringResource(R.string.pro_payment_details),
                fontSize = 12.5.sp,
                color = glassColors.textSecondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Restore purchases button
        TextButton(
            onClick = onRestorePurchasesClick,
            modifier = Modifier.testTag("btn_restore_purchases")
        ) {
            Text(
                text = stringResource(R.string.pro_restore_purchases),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AccentSunset
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Small Google Play disclaimer
        Text(
            text = stringResource(R.string.pro_google_play_secure),
            fontSize = 11.5.sp,
            color = glassColors.textSecondary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}
