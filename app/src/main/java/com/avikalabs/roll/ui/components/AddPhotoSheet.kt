package com.avikalabs.roll.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avikalabs.roll.ui.theme.AccentSunset
import com.avikalabs.roll.ui.theme.LocalGlassColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPhotoActionSheet(
    onDismiss: () -> Unit,
    onPhotosSelected: (List<Uri>) -> Unit,
    onFolderSelected: (Uri, String) -> Unit
) {
    val context = LocalContext.current
    val glassColors = LocalGlassColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Multiple photo picker launcher (system Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onPhotosSelected(uris)
            onDismiss()
        }
    }

    // Storage Access Framework folder launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                // Grant persistable permission per PRD §5 & §7.2.1
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)

                val docId = DocumentsContract.getTreeDocumentId(uri)
                val folderName = docId.substringAfterLast(":", docId).substringAfterLast("/", docId)
                onFolderSelected(uri, folderName.ifEmpty { "Folder" })
            } catch (e: Exception) {
                e.printStackTrace()
                onFolderSelected(uri, "Folder")
            }
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (glassColors.isDark) glassColors.glassFillElevated else glassColors.glassFillElevated,
        contentColor = glassColors.textPrimary,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Add photos to roll",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = glassColors.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
            )

            // Option 1: Choose photos
            ActionSheetRow(
                title = "Choose photos",
                icon = Icons.Outlined.PhotoLibrary,
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                testTag = "action_choose_photos"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Option 2: Choose a folder
            ActionSheetRow(
                title = "Choose a folder",
                icon = Icons.Outlined.Folder,
                onClick = {
                    folderPickerLauncher.launch(null)
                },
                testTag = "action_choose_folder"
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = glassColors.glassBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // Cancel
            ActionSheetRow(
                title = "Cancel",
                icon = Icons.Outlined.Close,
                onClick = onDismiss,
                isCancel = true,
                testTag = "action_cancel"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActionSheetRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isCancel: Boolean = false,
    testTag: String = ""
) {
    val glassColors = LocalGlassColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCancel) glassColors.glassFill.copy(alpha = 0.2f) else glassColors.glassFill)
            .clickable { onClick() }
            .padding(horizontal = 16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isCancel) glassColors.textSecondary else AccentSunset,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = if (isCancel) FontWeight.Normal else FontWeight.Medium,
            color = if (isCancel) glassColors.textSecondary else glassColors.textPrimary
        )
    }
}
