package es.uc3m.android.chillmates.model

const val REPAIRS_SUBCOLLECTION = "repairs"

data class Repair(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val reportedBy: String = "",
    val landlordMessage: String = "",
    val photoUris: List<String> = emptyList(),
    val status: String = "PENDING"
)
