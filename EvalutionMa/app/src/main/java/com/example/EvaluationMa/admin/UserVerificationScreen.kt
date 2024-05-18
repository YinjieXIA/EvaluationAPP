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
                unverifiedUsers = result.documents.mapNotNull { it.data }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching users: $e"
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
                        db.collection("users").document(userId)
                            .update(mapOf("verified" to true, "role" to role))
                            .addOnSuccessListener {
                                unverifiedUsers = unverifiedUsers.filterNot { it["uid"] == userId }
                            }
                            .addOnFailureListener { e ->
                                errorMessage = "Error verifying user: $e"
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
    val roles = listOf("student", "tutor", "client", "component_manager", "module_manager")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(email)
        DropdownMenu(
            expanded = true,
            onDismissRequest = { /* Handle dismiss request */ },
        ) {
            roles.forEach { role ->
                DropdownMenuItem(onClick = { selectedRole = role }) {
                    Text(role)
                }
            }
        }
        Button(onClick = { onVerify(userId, selectedRole) }) {
            Text("Verify")
        }
    }
}
