package com.example.tournote.Functionality.Repository

import android.util.Log
import com.example.tournote.GlobalClass
import com.example.tournote.GroupData_Detailed_Model
import com.example.tournote.UserModel
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.coroutines.tasks.await

class MainActivityRepository {

    val db = Firebase.database

    suspend fun groupData(groupId: String): Result<GroupData_Detailed_Model> {
        return try {
            val groupRef = db.getReference("groups").child(groupId)

            // 1. Fetch GroupDetails
            val groupDetailsSnap = groupRef.child("GroupDetails").get().await()
            val groupDetails = groupDetailsSnap.value as? Map<*, *> ?: return Result.failure(Exception("Invalid GroupDetails"))

            val name = groupDetails["name"] as? String
            val isGroupValid = groupDetails["isGroupValid"] as? Boolean
            val description = groupDetails["description"] as? String
            val profilePic = groupDetails["profilePic"] as? String
            val ownerID = groupDetails["owner"] as? String
            val createdAt = groupDetails["createdAt"] as? Long
            val groupID = groupDetails["groupId"] as? String ?: groupId

            // 2. Get raw member/admin/trackFriends email keys (unsanitized, with `,`)
            val rawMemberKeys = groupRef.child("Members").get().await().children.mapNotNull { it.key }
            val rawAdminKeys = groupRef.child("Admins").get().await().children.mapNotNull { it.key }
            // NEW: Fetch raw track friends keys
            val rawTrackFriendKeys = groupRef.child("TrackFriends").get().await().children.mapNotNull { it.key }


            // 3. Sanitize keys
            val memberEmails = rawMemberKeys.map { it.replace(",", ".") }
            val adminEmails = rawAdminKeys.map { it.replace(",", ".") }
            val ownerEmail = ownerID?.replace(",",".")
            // NEW: Sanitize track friends keys directly into the list for the model
            val trackFriendEmails = rawTrackFriendKeys.map { it.replace(",", ".") }


            // 4. Fetch all users and match by email for members, admins, and owner
            val usersSnap = db.getReference("users").get().await()
            val membersList = mutableListOf<UserModel>()
            val adminsList = mutableListOf<UserModel>()
            var owner : UserModel?=null

            usersSnap.children.forEach { userSnap ->
                val personalDetails = userSnap.child("PersonalDetails")
                val email = personalDetails.child("email").getValue(String::class.java)

                if (email != null) {
                    val user = UserModel(
                        uid = userSnap.key,
                        email = email,
                        name = personalDetails.child("name").getValue(String::class.java),
                        phoneNumber = personalDetails.child("phone").getValue(String::class.java),
                        profilePic = personalDetails.child("profilePic").getValue(String::class.java)
                    )

                    if (email in memberEmails) membersList.add(user)
                    if (email in adminEmails) adminsList.add(user)
                    if (email == ownerEmail) owner = user
                    // No need to add to trackFriendsList<UserModel> here, as it's now List<String>
                }
            }

            val group = GroupData_Detailed_Model(
                groupID = groupID,
                name = name,
                description = description,
                profilePic = profilePic,
                isGroupValid = isGroupValid,
                owner = owner!!,
                createdAt = createdAt,
                members = membersList,
                admins = adminsList,
                trackFriends = trackFriendEmails // Assign the list of sanitized emails directly
            )

            // ✅ Log all group details (uncomment if needed)
            //logGroupInfo(group)

            Result.success(group)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getUserByMailId(emailId: String): Result<UserModel> {
        return try {
            val usersSnap = db.getReference("users").get().await()

            usersSnap.children.forEach { userSnap ->
                val personalDetails = userSnap.child("PersonalDetails")
                val email = personalDetails.child("email").getValue(String::class.java)

                if (email == emailId) {
                    val user = UserModel(
                        uid = userSnap.key,
                        email = email,
                        name = personalDetails.child("name").getValue(String::class.java),
                        phoneNumber = personalDetails.child("phone").getValue(String::class.java),
                        profilePic = personalDetails.child("profilePic").getValue(String::class.java)
                    )
                    return Result.success(user)
                }
            }

            Result.failure(Exception("User with email $emailId not found"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    suspend fun EnableMyTrackingOnCurrentGroup() {
        try {
            val groupID = GlobalClass.GroupDetails_Everything.groupID ?: return
            val uID = GlobalClass.Me?.uid ?: return
            val email = GlobalClass.Me?.email?.replace(".",",") ?: return  // prevent null call

            val groupRef = db.getReference("groups").child(groupID).child("TrackFriends")
            val userRef = db.getReference("users").child(uID).child("GroupsTrackingMe")
            groupRef.child(email).setValue(true).await()
            userRef.child(groupID).setValue(true).await()

        } catch (e: Exception) {
            Log.e("EnableTracking", "Error enabling tracking: ${e.message}")
        }
    }

    suspend fun DisableMyTrackingOnCurrentGroup() {
        try {
            val groupID = GlobalClass.GroupDetails_Everything.groupID ?: return
            val uID = GlobalClass.Me?.uid ?: return
            val email = GlobalClass.Me?.email?.replace(".",",") ?: return

            val groupRef = db.getReference("groups").child(groupID).child("TrackFriends")
            val userRef = db.getReference("users").child(uID).child("GroupsTrackingMe")
            groupRef.child(email).removeValue().await()
            userRef.child(groupID).removeValue().await()

        } catch (e: Exception) {
            Log.e("DisableTracking", "Error disabling tracking: ${e.message}")
        }
    }



    suspend fun LeaveCurrentGroup() { // Parameter removed
        // Ensure GlobalClass.Me and its email are available
        val currentUser = GlobalClass.Me
        val currentUserEmail = currentUser?.email
        val currentUserId = currentUser?.uid

        if (currentUserEmail == null || currentUserId == null) {
            println("Error: Current user email or UID is null. Cannot leave group.")
            return // Cannot proceed without user info
        }

        // --- Fetch groupId from GlobalClass ---
        val grpId = GlobalClass.GroupDetails_Everything.groupID

        if (grpId == null) {
            Log.d("LeaveCurrentGroup", "Error: Group ID in GlobalClass.GroupDetails_Everything is null. Cannot leave group.")
            return
        }

        // Sanitize the email for Firebase keys (replace . with ,)
        val sanitizedEmail = currentUserEmail.replace(".", ",")

        try {
            val groupRef = db.getReference("groups").child(grpId)
            val userRef = db.getReference("users").child(currentUserId)

            // --- 1. Remove user from Members ---
            val memberRef = groupRef.child("Members").child(sanitizedEmail)
            memberRef.removeValue().await()


            // --- 2. Remove user from Admins (if present) ---
            val adminRef = groupRef.child("Admins").child(sanitizedEmail)
            adminRef.removeValue().await()


            // --- 3. Remove user from TrackFriends (if present) ---
            val trackFriendsRef = groupRef.child("TrackFriends").child(sanitizedEmail)
            trackFriendsRef.removeValue().await()


            // --- 4. Remove group ID from user's Groups section ---
            val userGroupsRef = userRef.child("Groups").child(grpId)
            userGroupsRef.removeValue().await()


            // --- 5. Update GlobalClass.GroupDetails_Everything ---
            // IMPORTANT: Ensure GlobalClass.GroupDetails_Everything is the correct group
            // and its groupID matches, though we are now directly using its groupID.
            // This check is still good practice to confirm you're modifying the intended global state.
            if (GlobalClass.GroupDetails_Everything.groupID == grpId) {

                val currentMembers = GlobalClass.GroupDetails_Everything.members.toMutableList()
                currentMembers.removeAll { it.email == currentUserEmail }

                val currentAdmins = GlobalClass.GroupDetails_Everything.admins.toMutableList()
                currentAdmins.removeAll { it.email == currentUserEmail }

                val currentTrackFriends = GlobalClass.GroupDetails_Everything.trackFriends?.toMutableList() ?: mutableListOf()
                currentTrackFriends.remove(currentUserEmail)

                // Create a new GroupData_Detailed_Model instance with updated lists
                GlobalClass.GroupDetails_Everything = GlobalClass.GroupDetails_Everything.copy(
                    members = currentMembers,
                    admins = currentAdmins,
                    trackFriends = currentTrackFriends
                )
            } else {
                // This scenario should ideally not happen if GlobalClass is kept consistent,
                // but it's a fallback.
                Log.d("LeaveCurrentGroup", "Warning: GlobalClass.GroupDetails_Everything groupID mismatch. Local state might be inconsistent.")
            }

        } catch (e: Exception) {
            Log.d("LeaveCurrentGroup", "Error leaving group $grpId: ${e.message}")
            throw e
        }
    }

    suspend fun DeleteCurrentGroupFromRoot(){
        // --- Fetch groupId from GlobalClass ---
        val grpId = GlobalClass.GroupDetails_Everything.groupID

        if (grpId == null) {
            Log.d("DeleteCurrentGroupFromRoot", "Error: Group ID in GlobalClass.GroupDetails_Everything is null. Cannot delete group.")
            return
        }

        try {
            val groupRef = db.getReference("groups").child(grpId)

            // --- 1. Get all members' emails (including owner and admins) to update their user branches ---
            // We need to fetch the current state of members, admins, and owner to get their UIDs.
            // It's safer to re-fetch this rather than relying solely on GlobalClass.GroupDetails_Everything
            // in case it's stale, especially for a destructive operation like delete.

            // Fetch current group details from Firebase for precise member UIDs
            val groupDetailsResult = groupData(grpId) // Re-use your existing groupData function
            if (groupDetailsResult.isFailure) {
                Log.e("DeleteCurrentGroupFromRoot", "Failed to fetch group details for deletion: ${groupDetailsResult.exceptionOrNull()?.message}")
                return // Cannot proceed without member info
            }
            val groupData = groupDetailsResult.getOrThrow()

            val allGroupMembers = mutableSetOf<String>() // Use Set for unique UIDs
            allGroupMembers.add(groupData.owner.uid!!) // Owner's UID
            groupData.members.forEach { it.uid?.let { uid -> allGroupMembers.add(uid) } }
            groupData.admins.forEach { it.uid?.let { uid -> allGroupMembers.add(uid) } }

            // --- 2. Remove the group ID from each member's /users/{uid}/Groups branch ---
            val userGroupsUpdates = mutableMapOf<String, Any?>()
            for (memberUid in allGroupMembers) {
                userGroupsUpdates["users/$memberUid/Groups/$grpId"] = null
            }
            // Perform a multi-path update for efficiency
            if (userGroupsUpdates.isNotEmpty()) {
                db.reference.updateChildren(userGroupsUpdates).await()
                Log.d("DeleteCurrentGroupFromRoot", "Removed group $grpId from ${allGroupMembers.size} user's Groups branches.")
            }


            // --- 3. Delete the group from the /groups root ---
            groupRef.removeValue().await()
            Log.d("DeleteCurrentGroupFromRoot", "Deleted group $grpId from /groups root.")

            // --- 4. Clear GlobalClass.GroupDetails_Everything if it was the deleted group ---
            if (GlobalClass.GroupDetails_Everything.groupID == grpId) {
                // Reset it to a default or null state, depending on how you manage initial group load
                // For simplicity, let's set it to a default empty/null state if your model allows.
                // Assuming you have a way to reset GlobalClass.GroupDetails_Everything to an initial state,
                // or if it's okay to have its fields be null after deletion.
                // If it must always contain valid data, you might set it to a "dummy" empty group.
                // For now, setting owner to a dummy UserModel is necessary since it's non-nullable.
                GlobalClass.GroupDetails_Everything = GroupData_Detailed_Model(
                    groupID = null,
                    name = null,
                    description = null,
                    profilePic = null,
                    owner = UserModel(), // Provide a default/empty UserModel instance
                    createdAt = null,
                    isGroupValid = false,
                    members = emptyList(),
                    admins = emptyList(),
                    trackFriends = emptyList()
                )
                Log.d("DeleteCurrentGroupFromRoot", "GlobalClass.GroupDetails_Everything cleared as group $grpId was deleted.")
            }

        }catch (e: Exception){
            Log.d("DeleteCurrentGroupFromRoot", "Error deleting group $grpId: ${e.message}")
            throw e
        }
    }

    suspend fun AddMemberToGroup(UserInfo: UserModel) {
        try {
            // Get group ID from GlobalClass
            val grpId = GlobalClass.GroupDetails_Everything.groupID

            if (grpId == null) {
                Log.d("AddMemberToGroup", "Error: Group ID in GlobalClass.GroupDetails_Everything is null. Cannot add member.")
                return
            }

            // Get user email and UID
            val userEmail = UserInfo.email
            val userUid = UserInfo.uid

            if (userEmail == null || userUid == null) {
                Log.d("AddMemberToGroup", "Error: User email or UID is null. Cannot add member.")
                return
            }

            // Sanitize email for Firebase keys (replace . with ,)
            val sanitizedEmail = userEmail.replace(".", ",")

            // Firebase references
            val groupRef = db.getReference("groups").child(grpId)
            val userRef = db.getReference("users").child(userUid)

            // --- 1. Add user to group's Members section ---
            val memberRef = groupRef.child("Members").child(sanitizedEmail)
            memberRef.setValue(true).await()

            // --- 2. Add group ID to user's Groups section ---
            val userGroupsRef = userRef.child("Groups").child(grpId)
            userGroupsRef.setValue(true).await()

            // --- 3. Update GlobalClass.GroupDetails_Everything ---
            // Check if the user is already a member to avoid duplicates
            val currentMembers = GlobalClass.GroupDetails_Everything.members.toMutableList()
            val isAlreadyMember = currentMembers.any { it.email == userEmail }

            if (!isAlreadyMember) {
                currentMembers.add(UserInfo)

                // Create a new GroupData_Detailed_Model instance with updated members list
                GlobalClass.GroupDetails_Everything = GlobalClass.GroupDetails_Everything.copy(
                    members = currentMembers
                )

                Log.d("AddMemberToGroup", "Successfully added ${UserInfo.name} (${UserInfo.email}) to group $grpId")
            } else {
                Log.d("AddMemberToGroup", "User ${UserInfo.email} is already a member of group $grpId")
            }

        } catch (e: Exception) {
            Log.e("AddMemberToGroup", "Error adding member to group: ${e.message}")
            throw e
        }
    }

    suspend fun EndTour(){
        // --- Fetch groupId from GlobalClass ---
        val grpId = GlobalClass.GroupDetails_Everything.groupID

        if (grpId == null) {
            Log.d("EndTour", "Error: Group ID in GlobalClass.GroupDetails_Everything is null. Cannot EndTour for the group.")
            return
        }

        try {
            val groupRef = db.getReference("groups").child(grpId)

            groupRef.child("GroupDetails").child("isGroupValid").setValue(false)
            groupRef.child("TrackFriends").removeValue().await()

            GlobalClass.GroupDetails_Everything.isGroupValid=false

            GlobalClass.GroupDetails_Everything=GlobalClass.GroupDetails_Everything.copy(trackFriends = emptyList())

            Log.d("DeleteCurrentGroupFromRoot", "Deleted group $grpId from /groups root.")

        }
        catch (e: Exception){
            Log.d("EndTour", "Error ending tour for group $grpId: ${e.message}")
            throw e
        }

        }


    /*private fun logGroupInfo(group: GroupData_Detailed_Model) {
        Log.d("GroupLog", "======= Group Info =======")
        Log.d("GroupLog", "Group ID: ${group.groupID}")
        Log.d("GroupLog", "Name: ${group.name}")
        Log.d("GroupLog", "Description: ${group.description}")
        Log.d("GroupLog", "Created At: ${group.createdAt}")
        Log.d("GroupLog", "Profile Pic: ${group.profilePic}")

        Log.d("GroupLog", "----- Owner -----")
        Log.d("GroupLog", "• ${group.owner.name} | ${group.owner.email} | ${group.owner.phoneNumber} | ${group.owner.profilePic}")

        Log.d("GroupLog", "----- Members -----")
        group.members.forEach {
            Log.d("GroupLog", "• ${it.name} | ${it.email} | ${it.phoneNumber} | ${it.profilePic}")
        }

        Log.d("GroupLog", "----- Admins -----")
        group.admins.forEach {
            Log.d("GroupLog", "• ${it.name} | ${it.email} | ${it.phoneNumber} | ${it.profilePic}")
        }

        Log.d("GroupLog", "==========================")
    }*/


}