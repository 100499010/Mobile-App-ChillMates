package es.uc3m.android.chillmates.model

import com.google.firebase.firestore.Exclude
import com.google.android.gms.maps.model.LatLng

const val SHOPPING_SUBCOLLECTION = "shoppingItems"

data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    val quantity: String = "",
    val neededBy: List<String> = emptyList(),
    val supermarkets: List<String> = emptyList(),
    val bought: Boolean = false
)

data class Supermarket(
    @get:Exclude var id: String? = null,
    val name: String = "",
    val officialName: String = "",
    val placeId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = ""
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
    override fun toString(): String = name
}