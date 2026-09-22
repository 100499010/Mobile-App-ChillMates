package es.uc3m.android.chillmates.model

const val FLATS_COLLECTION = "flats"

/**
 * Represents the main document of a shared flat.
 * Other entities like Chores or Expenses are now stored in subcollections.
 */
data class Flat(
    var id: String? = null,
    val name: String = "",
    val inviteCode: String = "",
    val memberIds: List<String> = emptyList(),
    val supermarketIds: List<String> = emptyList(),
    var lastRotationTimestamp: Long = 0L
)