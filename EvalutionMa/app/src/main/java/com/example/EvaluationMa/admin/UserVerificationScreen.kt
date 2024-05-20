package com.example.EvaluationMa.admin

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

@Composable
fun UserVerificationScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var unverifiedUsers by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("verified", false)
            .get()
            .addOnSuccessListener { result ->
                unverifiedUsers = result.documents.mapNotNull { document ->
                    val data = document.data
                    data?.put("uid", document.id)
                    data
                }
                Log.d("UserVerificationScreen", "Fetched users: $unverifiedUsers")
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching users: $e"
                Log.e("UserVerificationScreen", errorMessage)
            }
    }

    fun generateStudentId(onSuccess: (Int) -> Unit, onFailure: (Exception) -> Unit) {
        val counterRef = db.collection("studentIdCounter").document("counter")

        db.runTransaction { transaction ->
            val snapshot = transaction.get(counterRef)
            val currentId = snapshot.getLong("currentId")?.toInt() ?: 1000
            val newId = currentId + 1
            transaction.update(counterRef, "currentId", newId)
            newId
        }.addOnSuccessListener { newId ->
            onSuccess(newId)
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("User Verification", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (unverifiedUsers.isNotEmpty()) {
            LazyColumn {
                items(unverifiedUsers) { user ->
                    UserItem(user, onVerify = { userId, role ->
                        if (role == "student") {
                            generateStudentId(
                                onSuccess = { studentId ->
                                    db.collection("users").document(userId)
                                        .update(mapOf("verified" to true, "role" to role, "studentId" to studentId))
                                        .addOnSuccessListener {
                                            unverifiedUsers = unverifiedUsers.filterNot { it["uid"] == userId }
                                            Log.d("UserVerificationScreen", "Verified student user: $userId with studentId: $studentId")
                                        }
                                        .addOnFailureListener { e ->
                                            errorMessage = "Error verifying student user: $e"
                                            Log.e("UserVerificationScreen", errorMessage)
                                        }
                                },
                                onFailure = { e ->
                                    errorMessage = "Error generating studentId: $e"
                                    Log.e("UserVerificationScreen", errorMessage)
                                }
                            )
                        } else {
                            db.collection("users").document(userId)
                                .update(mapOf("verified" to true, "role" to role))
                                .addOnSuccessListener {
                                    unverifiedUsers = unverifiedUsers.filterNot { it["uid"] == userId }
                                    Log.d("UserVerificationScreen", "Verified user: $userId with role: $role")
                                }
                                .addOnFailureListener { e ->
                                    errorMessage = "Error verifying user: $e"
                                    Log.e("UserVerificationScreen", errorMessage)
                                }
                        }
                    })
                }
            }
        } else {
            Text("No unverified users.")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun UserItem(user: Map<String, Any>, onVerify: (String, String) -> Unit) {
    val userId = user["uid"] as? String ?: ""
    val email = user["email"] as? String ?: ""
    var selectedRole by remember { mutableStateOf("student") }
    var expanded by remember { mutableStateOf(false) }
    val roles = listOf("student", "tutor", "client", "component_manager", "module_manager")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(email, modifier = Modifier.weight(1f))

        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            TextButton(onClick = { expanded = true }) {
                Text(selectedRole)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                roles.forEach { role ->
                    DropdownMenuItem(onClick = {
                        selectedRole = role
                        expanded = false
                    }) {
                        Text(role)
                    }
                }
            }
        }

        Button(onClick = {
            if (userId.isNotEmpty()) {
                onVerify(userId, selectedRole)
            } else {
                Log.e("UserItem", "User ID is empty for user: $email")
            }
        }) {
            Text("Verify")
        }
    }
}
