package es.uc3m.android.chillmates.repairs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.chillmates.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import es.uc3m.android.chillmates.model.Repair
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import es.uc3m.android.chillmates.repository.FlatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class RepairUi(
    val id: String,
    val title: String,
    val description: String,
    val reportedBy: String,
    val landlordMessage: String,
    val photoUris: List<String>,
    val status: String
)

data class RepairUiState(
    val repairs: List<RepairUi> = emptyList(),
    val currentUserName: String = "Me"
)

class RepairViewModel(
    private val repository: FlatRepository = FlatRepository()
) : ViewModel() {

    var uiState by mutableStateOf(RepairUiState())
        private set

    var isAiReviewing by mutableStateOf(false)
        private set

    private var currentFlatId: String? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    init {
        loadUserAndFlatData()
    }

    private fun loadUserAndFlatData() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            try {
                val userDoc = db.collection(USERS_COLLECTION).document(userId).get().await()
                val flatId = userDoc.getString("flatId") ?: return@launch
                val userName = userDoc.getString("displayName") ?: "Me"

                currentFlatId = flatId
                uiState = uiState.copy(currentUserName = userName)

                launch {
                    repository.observeRepairs(flatId).collect { repairsList ->
                        uiState = uiState.copy(
                            repairs = repairsList.map { it.toUi() }
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reviewMessageWithAI(rawDescription: String, onResult: (String) -> Unit) {
        val cleaned = rawDescription.trim()
        if (cleaned.isBlank()) return

        viewModelScope.launch {
            isAiReviewing = true
            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = BuildConfig.GEMINI_API_KEY
                )

                val prompt = """
                    You are a polite and professional assistant helping a tenant write a maintenance request to their landlord.
                    The tenant's name is ${uiState.currentUserName}.
                    
                    Please rewrite the following issue description into a clear, formal, and polite email body.
                    Fix any spelling or grammar mistakes.
                    Do NOT add any conversational filler (like "Here is your email"), just return the finalized email text.
                    Do NOT include the subject and landlord's name, but use the subject to get additional information.
                    Do NOT use any placeholder, if you do not have enough information, use vague words.
                    Here is the issue description:
                    $cleaned
                """.trimIndent()

                val response = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(prompt)
                }

                response.text?.let { improvedText ->
                    onResult(improvedText.trim())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val fallbackText = """
                    Hello,

                    I am ${uiState.currentUserName} from the flat.

                    We would like to report the following maintenance issue:
                    - ${cleaned.replaceFirstChar { it.uppercase() }}

                    Could you please let us know how to proceed to get this resolved?

                    Best regards,
                    ${uiState.currentUserName}
                """.trimIndent()
                onResult(fallbackText)
            } finally {
                isAiReviewing = false
            }
        }
    }

    fun createSentRepair(title: String, rawDescription: String, finalMessage: String, photoUris: List<String>) {
        val flatId = currentFlatId ?: return
        val description = rawDescription.trim()
        if (description.isBlank()) return

        val repairTitle = title.trim().ifBlank { "Repair request" }
        val repairId = System.currentTimeMillis().toString()

        viewModelScope.launch {
            val savedPhotoUris = uploadRepairPhotos(
                flatId = flatId,
                repairId = repairId,
                photoUris = photoUris
            )

            val newRepair = Repair(
                id = repairId,
                title = repairTitle,
                description = description,
                reportedBy = uiState.currentUserName,
                landlordMessage = finalMessage.trim().ifBlank { description },
                photoUris = savedPhotoUris,
                status = "SENT"
            )

            repository.addRepair(flatId, newRepair)
        }
    }

    private suspend fun uploadRepairPhotos(
        flatId: String,
        repairId: String,
        photoUris: List<String>
    ): List<String> {
        return withContext(Dispatchers.IO) {
            photoUris.mapIndexedNotNull { index, photoUri ->
                try {
                    val uri = Uri.parse(photoUri)
                    val photoRef = storage.reference
                        .child("flats")
                        .child(flatId)
                        .child("repairs")
                        .child(repairId)
                        .child("photo_$index.jpg")

                    photoRef.putFile(uri).await()
                    photoRef.downloadUrl.await().toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }

    fun updateRepairStatus(repairId: String, newStatus: String) {
        val flatId = currentFlatId ?: return
        viewModelScope.launch {
            repository.updateRepairStatus(flatId, repairId, newStatus)
        }
    }

    private fun Repair.toUi(): RepairUi =
        RepairUi(id = id, title = title, description = description, reportedBy = reportedBy, landlordMessage = landlordMessage, photoUris = photoUris, status = status)
}