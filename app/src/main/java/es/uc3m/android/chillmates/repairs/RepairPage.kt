package es.uc3m.android.chillmates.repairs

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import es.uc3m.android.chillmates.LargeSectionHeader
import es.uc3m.android.chillmates.PageContentWrapper

private const val LANDLORD_EMAIL = "landlord@chillmates.app"
private const val GMAIL_PACKAGE = "com.google.android.gm"
private const val REPAIR_SUBJECT = "Repair Request: Shared Flat"

private data class PendingRepairDraft(
    val title: String,
    val rawDescription: String,
    val finalMessage: String,
    val photoUris: List<String>
)

@Composable
fun RepairPage(navController: NavController) {
    val vm: RepairViewModel = viewModel()
    val context = LocalContext.current

    var requestText by rememberSaveable { mutableStateOf("") }
    var repairTitle by rememberSaveable { mutableStateOf("") }

    val selectedPhotoUris = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { restored -> mutableStateListOf<String>().apply { addAll(restored) } }
        )
    ) {
        mutableStateListOf<String>()
    }

    var pendingDraft by remember { mutableStateOf<PendingRepairDraft?>(null) }
    var showSentConfirmationDialog by remember { mutableStateOf(false) }
    var showRepairForm by rememberSaveable { mutableStateOf(false) }

    val pickImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }

            val uriString = uri.toString()
            if (uriString !in selectedPhotoUris) {
                selectedPhotoUris.add(uriString)
            }
        }
    }

    val gmailLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (pendingDraft != null) {
            showSentConfirmationDialog = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LargeSectionHeader(title = "Repairs")

        PageContentWrapper {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(vm.uiState.repairs) { repair ->
                        RepairRequestCard(
                            repair = repair,
                            onStatusSelected = { status ->
                                vm.updateRepairStatus(repair.id, status)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showRepairForm = !showRepairForm },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = if (showRepairForm) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (showRepairForm) "Hide request form" else "New repair request")
                }

                if (showRepairForm) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Send a new request",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        TextField(
                            value = repairTitle,
                            onValueChange = { repairTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Repair title...") },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            TextField(
                                value = requestText,
                                onValueChange = { requestText = it },
                                modifier = Modifier.fillMaxSize(),
                                placeholder = { Text("Describe the issue...") }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = { pickImagesLauncher.launch(arrayOf("image/*")) }
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (selectedPhotoUris.isEmpty()) "Add images"
                                else "Add more images (${selectedPhotoUris.size} selected)"
                            )
                        }

                        if (selectedPhotoUris.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            SelectedImagesPreview(
                                photoUris = selectedPhotoUris,
                                onRemovePhoto = { uriToRemove ->
                                    selectedPhotoUris.remove(uriToRemove)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    vm.reviewMessageWithAI(requestText) { improvedText ->
                                        requestText = improvedText
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(50),
                                enabled = requestText.isNotBlank() && !vm.isAiReviewing
                            ) {
                                if (vm.isAiReviewing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Thinking...")
                                } else {
                                    Text("AI Review")
                                }
                            }

                            Button(
                                onClick = {
                                    val trimmedTitle = repairTitle.trim()
                                    val trimmedText = requestText.trim()
                                    if (trimmedTitle.isBlank() || trimmedText.isBlank()) return@Button

                                    val draft = PendingRepairDraft(
                                        title = trimmedTitle,
                                        rawDescription = trimmedText,
                                        finalMessage = trimmedText,
                                        photoUris = selectedPhotoUris.toList()
                                    )

                                    val emailIntent = buildGmailIntent(
                                        context = context,
                                        to = LANDLORD_EMAIL,
                                        subject = "$REPAIR_SUBJECT: ${draft.title}",
                                        body = draft.finalMessage,
                                        attachmentUris = draft.photoUris.map(Uri::parse)
                                    )

                                    pendingDraft = draft

                                    try {
                                        gmailLauncher.launch(emailIntent)
                                    } catch (_: ActivityNotFoundException) {
                                        pendingDraft = null
                                        Toast.makeText(
                                            context,
                                            "Gmail is not installed on this device",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(50),
                                enabled = repairTitle.isNotBlank() && requestText.isNotBlank() && !vm.isAiReviewing
                            ) {
                                Text("Send")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSentConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showSentConfirmationDialog = false },
            title = { Text("Confirm email sending") },
            text = { Text("Did you finally send the email from Gmail?") },
            confirmButton = {
                Button(
                    onClick = {
                        val draft = pendingDraft
                        if (draft != null) {
                            vm.createSentRepair(
                                title = draft.title,
                                rawDescription = draft.rawDescription,
                                finalMessage = draft.finalMessage,
                                photoUris = draft.photoUris
                            )

                            requestText = ""
                            repairTitle = ""
                            selectedPhotoUris.clear()
                        }

                        pendingDraft = null
                        showSentConfirmationDialog = false
                    }
                ) {
                    Text("Yes, sent")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDraft = null
                        showSentConfirmationDialog = false
                    }
                ) {
                    Text("No")
                }
            }
        )
    }
}

private fun buildGmailIntent(
    context: Context,
    to: String,
    subject: String,
    body: String,
    attachmentUris: List<Uri>
): Intent {
    return if (attachmentUris.isEmpty()) {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = GMAIL_PACKAGE
            putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            `package` = GMAIL_PACKAGE
            putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachmentUris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            clipData = ClipData.newUri(
                context.contentResolver,
                "repair_images",
                attachmentUris.first()
            ).apply {
                attachmentUris.drop(1).forEach { uri ->
                    addItem(ClipData.Item(uri))
                }
            }
        }
    }
}

@Composable
private fun SelectedImagesPreview(
    photoUris: List<String>,
    onRemovePhoto: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        photoUris.forEach { photoUri ->
            Box {
                AsyncImage(
                    model = photoUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentScale = ContentScale.Crop
                )

                IconButton(
                    onClick = { onRemovePhoto(photoUri) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(bottomStart = 12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove image",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RepairRequestCard(
    repair: RepairUi,
    onStatusSelected: (String) -> Unit
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(26.dp)
                    .padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = repair.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (repair.photoUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repair.photoUris.forEach { photoUri ->
                            AsyncImage(
                                model = photoUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box {
                    StatusChip(
                        status = repair.status,
                        onClick = { statusMenuExpanded = true }
                    )

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        listOf(
                            "PENDING",
                            "SENT",
                            "IN_PROGRESS",
                            "ACCEPTED",
                            "DENIED",
                            "RESOLVED"
                        ).forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    onStatusSelected(status)
                                    statusMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    status: String,
    onClick: () -> Unit
) {
    val background = when (status) {
        "PENDING" -> Color(0xFFD84C3E)
        "SENT" -> Color(0xFF4F6DB8)
        "IN_PROGRESS" -> Color(0xFF5A647A)
        "ACCEPTED" -> Color(0xFF2E8B57)
        "DENIED" -> Color(0xFF8B0000)
        "RESOLVED" -> Color(0xFF7B5A7A)
        else -> MaterialTheme.colorScheme.primary
    }

    TextButton(
        onClick = onClick,
        modifier = Modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 2.dp, vertical = 0.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = Color.White
        )
    ) {
        Text(
            text = status.replace("_", " "),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}