package com.avikalabs.roll.worker

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.avikalabs.roll.data.model.WallpaperItem
import com.avikalabs.roll.data.repository.RollRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FolderSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository = RollRepository.getInstance(applicationContext)
        val syncedFolders = repository.getAllSyncedFoldersSync()
        val removedUris = repository.getAllRemovedUris().toSet()

        for (folder in syncedFolders) {
            try {
                val treeUri = Uri.parse(folder.treeUri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )

                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                )

                val cursor = applicationContext.contentResolver.query(
                    childrenUri,
                    projection,
                    null,
                    null,
                    null
                )

                val existingItems = repository.getItemsByTargetSync(folder.target)
                val existingUris = existingItems.map { it.uri }.toSet()

                val newItems = mutableListOf<WallpaperItem>()
                var currentMaxSortIndex = (existingItems.maxOfOrNull { it.sortIndex } ?: -1) + 1

                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val mimeCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

                    while (c.moveToNext()) {
                        val mime = c.getString(mimeCol)
                        if (mime != null && mime.startsWith("image/")) {
                            val docId = c.getString(idCol)
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()

                            if (!existingUris.contains(docUri) && !removedUris.contains(docUri)) {
                                newItems.add(
                                    WallpaperItem(
                                        uri = docUri,
                                        target = folder.target,
                                        durationSeconds = 900,
                                        sourceFolderUri = folder.treeUri,
                                        sortIndex = currentMaxSortIndex++
                                    )
                                )
                            }
                        }
                    }
                }

                if (newItems.isNotEmpty()) {
                    repository.addItems(newItems)
                }
            } catch (e: SecurityException) {
                // Inaccessible folder edge case handled gracefully
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Result.success()
    }
}
