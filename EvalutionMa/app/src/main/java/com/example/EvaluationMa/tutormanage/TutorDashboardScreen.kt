package com.example.EvaluationMa.tutormanage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun TutorDashboardScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var groups by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            db.collection("groups")
                .get()
                .addOnSuccessListener { result ->
                    val groupList = result.documents.mapNotNull { document ->
                        val componentTutors = document.get("componentTutors") as? Map<*, *>
                        if (componentTutors?.containsValue(user.uid) == true) {
                            document.data?.apply { put("uid", document.id) }
                        } else {
                            null
                        }
                    }
                    groups = groupList
                }
                .addOnFailureListener { e ->
                    errorMessage = "Error fetching groups: $e"
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Tutor Dashboard", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("profile") }) {
            Text("Profile")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("announcement_screen") }) {
            Text("Announcement Management")
        }



        Spacer(modifier = Modifier.height(16.dp))

        if (groups.isNotEmpty()) {
            LazyColumn {
                items(groups) { group ->
                    val groupId = group["uid"] as String
                    val groupName = group["name"] as? String ?: "Unnamed Group"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                navController.navigate("tutor_group_components/$groupId")
                            },
                        elevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(groupName, style = MaterialTheme.typography.h6)
                        }
                    }
                }
            }
        } else {
            Text("No groups found.")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colors.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}