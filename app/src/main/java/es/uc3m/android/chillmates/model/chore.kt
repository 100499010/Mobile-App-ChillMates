package es.uc3m.android.chillmates.model

const val CHORES_SUBCOLLECTION = "chores"

data class ChoreAssignment(
    val assignee: String = "",
    val done: Boolean = false
)

data class Chore(
    val id: String = "",
    val title: String = "",
    val done: Boolean = false,
    val assignments: Map<String, ChoreAssignment> = emptyMap()
)