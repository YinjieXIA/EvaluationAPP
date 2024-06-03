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
fun TutorGroupComponentsScreen(navController: NavController, groupId: String) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    var components by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(groupId) {
        currentUser?.let { user ->
            db.collection("groups").document(groupId).get()
                .addOnSuccessListener { document ->
                    val componentTutors = document.get("componentTutors") as? Map<*, *>
                    val userComponents = componentTutors?.filterValues { it == user.uid }?.keys ?: emptySet<Any>()

                    db.collection("components")
                        .whereIn("id", userComponents.toList())
                        .get()
                        .addOnSuccessListener { componentResult ->
                            components = componentResult.documents.mapNotNull { componentDoc ->
                                componentDoc.data?.apply { put("uid", componentDoc.id) }
                            }
                        }
                        .addOnFailureListener { e ->
                            errorMessage = "Error fetching components: $e"
                        }
                }
                .addOnFailureListener { e ->
                    errorMessage = "Error fetching group details: $e"
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Components in Group", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (components.isNotEmpty()) {
            LazyColumn {
                items(components) { component ->
                    val componentId = component["uid"] as String
                    val componentName = component["name"] as String
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                navController.navigate("tutor_component_detail/$componentId/$groupId")
                            },
                        elevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(componentName, style = MaterialTheme.typography.h6)
                        }
                    }
                }
            }
        } else {
            Text("No components found.")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colors.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}