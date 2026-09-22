package es.uc3m.android.chillmates.model

const val NOTICES_SUBCOLLECTION = "notices"

data class Notice(
    val id: String = "",
    val title: String = "",
    val details: String = "",
    val createdBy: String = "",
    val acceptedBy: List<String> = emptyList(),
    val declinedBy: List<String> = emptyList(),
    val poll: Boolean = false
)