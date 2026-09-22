package es.uc3m.android.chillmates.model

data class User(
    var id: String? = null,
    var email: String = "",
    var displayName: String = "",
    var flatId: String? = null,
    var flatName: String? = null,
    var avatarUrl: String? = null,
    var joinedDate: Long = 0L,
    var notificationEnabled: Boolean = true
) {
    fun hasFlat(): Boolean = !flatId.isNullOrEmpty()
}

const val USERS_COLLECTION = "users"