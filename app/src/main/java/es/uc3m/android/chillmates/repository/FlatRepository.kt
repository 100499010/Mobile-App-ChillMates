package es.uc3m.android.chillmates.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import es.uc3m.android.chillmates.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class FlatRepository {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    suspend fun createFlat(name: String, creatorId: String): Result<Flat> {
        return try {
            val inviteCode = generateInviteCode()
            val flat = Flat(name = name, inviteCode = inviteCode, memberIds = listOf(creatorId))
            val docRef = firestore.collection(FLATS_COLLECTION).document()
            flat.id = docRef.id
            docRef.set(flat).await()
            firestore.collection(USERS_COLLECTION).document(creatorId)
                .update(mapOf("flatId" to docRef.id, "flatName" to name)).await()
            Result.success(flat)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getFlatByCode(inviteCode: String): Result<Flat> {
        return try {
            val snapshot = firestore.collection(FLATS_COLLECTION)
                .whereEqualTo("inviteCode", inviteCode.uppercase()).get().await()
            if (snapshot.isEmpty) return Result.failure(Exception("Flat not found"))
            val doc = snapshot.documents.first()
            val flat = doc.toObject(Flat::class.java)?.apply { id = doc.id } ?: throw Exception("Parse error")
            Result.success(flat)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun joinFlat(inviteCode: String, userId: String): Result<Flat> {
        return try {
            val flat = getFlatByCode(inviteCode).getOrThrow()
            if (flat.memberIds.contains(userId)) throw Exception("Already a member")
            firestore.collection(FLATS_COLLECTION).document(flat.id!!)
                .update("memberIds", FieldValue.arrayUnion(userId)).await()
            firestore.collection(USERS_COLLECTION).document(userId)
                .update(mapOf("flatId" to flat.id, "flatName" to flat.name)).await()
            Result.success(flat.copy(memberIds = flat.memberIds + userId))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun leaveFlat(flatId: String, userId: String): Result<Unit> {
        return try {
            firestore.runBatch { batch ->
                val flatRef = firestore.collection(FLATS_COLLECTION).document(flatId)
                val userRef = firestore.collection(USERS_COLLECTION).document(userId)

                batch.update(flatRef, "memberIds", FieldValue.arrayRemove(userId))
                batch.update(userRef, mapOf("flatId" to null, "flatName" to null))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeFlat(flatId: String): Flow<Flat?> = callbackFlow {
        val listener = firestore.collection(FLATS_COLLECTION).document(flatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot.toObject(Flat::class.java)?.apply { id = snapshot.id })
            }
        awaitClose { listener.remove() }
    }

    suspend fun getMemberDetails(memberIds: List<String>): Result<List<Pair<String, String>>> {
        return try {
            val members = memberIds.mapNotNull { userId ->
                try {
                    val doc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
                    userId to (doc.getString("displayName") ?: "Unknown")
                } catch (e: Exception) { null }
            }
            Result.success(members)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateFlatRotationTimestamp(flatId: String, timestamp: Long): Result<Unit> {
        return try {
            firestore.collection(FLATS_COLLECTION).document(flatId)
                .update("lastRotationTimestamp", timestamp).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getSupermarketsForFlat(flatId: String): Result<List<Supermarket>> {
        return try {
            val flatDoc = firestore.collection(FLATS_COLLECTION).document(flatId).get().await()
            val supermarketIds = (flatDoc.get("supermarketIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            if (supermarketIds.isEmpty()) return Result.success(emptyList())

            val supermarkets = supermarketIds.mapNotNull { supermarketId ->
                val marketDoc = firestore.collection("supermarkets").document(supermarketId).get().await()
                if (marketDoc.exists()) marketDoc.toObject(Supermarket::class.java)?.apply { id = marketDoc.id } else null
            }
            Result.success(supermarkets)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun addSupermarketForFlat(flatId: String, supermarket: Supermarket): Result<Supermarket> {
        return try {
            val docRef = firestore.collection("supermarkets").document()
            val toSave = supermarket.copy(id = docRef.id)
            firestore.runBatch { batch ->
                batch.set(docRef, toSave)
                batch.set(firestore.collection(FLATS_COLLECTION).document(flatId), mapOf("supermarketIds" to FieldValue.arrayUnion(docRef.id)), SetOptions.merge())
            }.await()
            Result.success(toSave)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun observeChores(flatId: String): Flow<List<Chore>> = callbackFlow {
        val listener = firestore.collection(FLATS_COLLECTION).document(flatId).collection(CHORES_SUBCOLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toObject(Chore::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun getChoresOnce(flatId: String): Result<List<Chore>> {
        return try {
            val snapshot = firestore.collection(FLATS_COLLECTION).document(flatId).collection(CHORES_SUBCOLLECTION).get().await()
            Result.success(snapshot.documents.mapNotNull { it.toObject(Chore::class.java) })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun addChore(flatId: String, chore: Chore) {
        firestore.collection(FLATS_COLLECTION).document(flatId).collection(CHORES_SUBCOLLECTION).document(chore.id).set(chore).await()
    }

    suspend fun updateChoreStatus(
        flatId: String,
        choreId: String,
        weekKey: String,
        done: Boolean,
        assignee: String
    ) {
        val choreRef = firestore.collection(FLATS_COLLECTION)
            .document(flatId)
            .collection(CHORES_SUBCOLLECTION)
            .document(choreId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(choreRef)

            val existingAssignments = snapshot.get("assignments") as? Map<String, Any> ?: emptyMap()

            val updatedWeekData = mapOf(
                "assignee" to assignee,
                "done" to done
            )

            val newAssignmentsMap = existingAssignments.toMutableMap()
            newAssignmentsMap[weekKey] = updatedWeekData

            transaction.update(choreRef, "done", done)
            transaction.update(choreRef, "assignments", newAssignmentsMap)

            null
        }.await()
    }

    suspend fun updateChoreAssignee(
        flatId: String,
        choreId: String,
        weekKey: String,
        newAssignee: String,
        done: Boolean
    ) {
        val choreRef = firestore.collection(FLATS_COLLECTION)
            .document(flatId)
            .collection(CHORES_SUBCOLLECTION)
            .document(choreId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(choreRef)

            val existingAssignments = snapshot.get("assignments") as? Map<String, Any> ?: emptyMap()

            val updatedWeekData = mapOf(
                "assignee" to newAssignee,
                "done" to done
            )

            val newAssignmentsMap = existingAssignments.toMutableMap()
            newAssignmentsMap[weekKey] = updatedWeekData

            transaction.update(choreRef, "assignee", newAssignee)
            transaction.update(choreRef, "assignments", newAssignmentsMap)

            null
        }.await()
    }

    suspend fun saveChoresBatch(flatId: String, chores: List<Chore>) {
        firestore.runBatch { batch ->
            chores.forEach {
                val ref = firestore.collection(FLATS_COLLECTION).document(flatId).collection(CHORES_SUBCOLLECTION).document(it.id)
                batch.set(ref, it)
            }
        }.await()
    }

    suspend fun deleteChore(flatId: String, choreId: String) {
        firestore.collection(FLATS_COLLECTION).document(flatId).collection(CHORES_SUBCOLLECTION).document(choreId).delete().await()
    }

    suspend fun performAutoRotationBatch(flatId: String, chores: List<Chore>, timestamp: Long) {
        firestore.runBatch { batch ->
            chores.forEach {
                val ref = firestore.collection(FLATS_COLLECTION).document(flatId).collection(CHORES_SUBCOLLECTION).document(it.id)
                batch.set(ref, it)
            }
            batch.update(firestore.collection(FLATS_COLLECTION).document(flatId), "lastRotationTimestamp", timestamp)
        }.await()
    }

    fun observeRepairs(flatId: String): Flow<List<Repair>> = callbackFlow {
        val listener = firestore.collection(FLATS_COLLECTION).document(flatId).collection(REPAIRS_SUBCOLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toObject(Repair::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun addRepair(flatId: String, repair: Repair) {
        firestore.collection(FLATS_COLLECTION).document(flatId).collection(REPAIRS_SUBCOLLECTION).document(repair.id).set(repair).await()
    }

    suspend fun updateRepairStatus(flatId: String, repairId: String, newStatus: String) {
        firestore.collection(FLATS_COLLECTION).document(flatId).collection(REPAIRS_SUBCOLLECTION).document(repairId).update("status", newStatus).await()
    }

    fun observeExpenses(flatId: String): Flow<List<Expense>> = callbackFlow {
        val listener = firestore.collection(FLATS_COLLECTION).document(flatId).collection(EXPENSES_SUBCOLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toObject(Expense::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun addExpense(flatId: String, expense: Expense) {
        firestore.collection(FLATS_COLLECTION).document(flatId).collection(EXPENSES_SUBCOLLECTION).document(expense.id).set(expense).await()
    }

    suspend fun updateExpenseTicket(flatId: String, expenseId: String, uriString: String) {
        firestore.collection(FLATS_COLLECTION).document(flatId).collection(EXPENSES_SUBCOLLECTION).document(expenseId).update("ticketUri", uriString).await()
    }

    fun observeShoppingItems(flatId: String): Flow<List<ShoppingItem>> = callbackFlow {
        val listener = firestore.collection(FLATS_COLLECTION).document(flatId).collection(SHOPPING_SUBCOLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toObject(ShoppingItem::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun addShoppingItem(flatId: String, item: ShoppingItem) {
        firestore.collection(FLATS_COLLECTION).document(flatId).collection(SHOPPING_SUBCOLLECTION).document(item.id).set(item).await()
    }

    suspend fun updateShoppingItemBought(flatId: String, itemId: String, bought: Boolean) {
        firestore.collection(FLATS_COLLECTION).document(flatId).collection(SHOPPING_SUBCOLLECTION).document(itemId).update("bought", bought).await()
    }

    suspend fun deleteShoppingItem(flatId: String, itemId: String) {
        firestore.collection(FLATS_COLLECTION).document(flatId).collection(SHOPPING_SUBCOLLECTION).document(itemId).delete().await()
    }

    fun observeNotices(flatId: String): Flow<List<Notice>> = callbackFlow {
        val listener = firestore.collection(FLATS_COLLECTION).document(flatId).collection(NOTICES_SUBCOLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toObject(Notice::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun addNotice(flatId: String, notice: Notice) {
        firestore.collection(FLATS_COLLECTION).document(flatId).collection(NOTICES_SUBCOLLECTION).document(notice.id).set(notice).await()
    }

    suspend fun updateNoticeAcceptedBy(flatId: String, noticeId: String, acceptedBy: List<String>) {
        firestore.collection(FLATS_COLLECTION).document(flatId).collection(NOTICES_SUBCOLLECTION).document(noticeId).update("acceptedBy", acceptedBy).await()
    }

    suspend fun updateNoticeDeclinedBy(flatId: String, noticeId: String, declinedBy: List<String>) {
        firestore.collection(FLATS_COLLECTION).document(flatId).collection(NOTICES_SUBCOLLECTION).document(noticeId).update("declinedBy", declinedBy).await()
    }

    fun observeEvents(flatId: String): Flow<List<UpcomingEvent>> = callbackFlow {
        val listener = firestore.collection(FLATS_COLLECTION).document(flatId).collection(EVENTS_SUBCOLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toObject(UpcomingEvent::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun addEvent(flatId: String, event: UpcomingEvent) {
        firestore.collection(FLATS_COLLECTION).document(flatId).collection(EVENTS_SUBCOLLECTION).document(event.id).set(event).await()
    }
}