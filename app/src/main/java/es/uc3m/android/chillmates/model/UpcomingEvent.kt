package es.uc3m.android.chillmates.model

const val EVENTS_SUBCOLLECTION = "events"

data class UpcomingEvent(
    val id: String = "",
    val dateLabel: String = "",
    val description: String = "",
    val location: String = ""
)