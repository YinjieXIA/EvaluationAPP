package com.example.EvaluationMa.admin.studentManage

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun StudentDetailScreen(navController: NavController, studentId: String) {
    val db = FirebaseFirestore.getInstance()
    var student by remember { mutableStateOf<Map<String, Any>?>(null) }
    var groups by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var teams by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var selectedTeam by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(studentId) {
        db.collection("users").document(studentId).get()
            .addOnSuccessListener { document ->
                if (document.getString("role") == "student") {
                    student = document.data
                } else {
                    errorMessage = "The specified user is not a student."
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching student: $e"
            }

        db.collection("groups").get()
            .addOnSuccessListener { result ->
                groups = result.documents.mapNotNull { document ->
                    val data = document.data
                    data?.put("uid", document.id)
                    data
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching groups: $e"
            }
    }

    fun loadTeams(groupId: String) {
        db.collection("groups").document(groupId).collection("teams").get()
            .addOnSuccessListener { result ->
                teams = result.documents.mapNotNull { document ->
                    val data = document.data
                    data?.put("uid", document.id)
                    data
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching teams: $e"
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Student Detail", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (student != null) {
            Text("Name: ${student!!["firstName"]} ${student!!["lastName"]}")
            Spacer(modifier = Modifier.height(16.dp))
            Text("Current Group: ${student!!["group"] ?: "None"}")
            Spacer(modifier = Modifier.height(16.dp))
            Text("Current Team: ${student!!["team"] ?: "None"}")
            Spacer(modifier = Modifier.height(16.dp))

            Text("Change Group")
            Spacer(modifier = Modifier.height(8.dp))

            DropdownMenu(
                expanded = selectedGroup != null,
                onDismissRequest = { selectedGroup = null }
            ) {
                groups.forEach { group ->
                    DropdownMenuItem(onClick = {
                        selectedGroup = group["uid"] as String
                        loadTeams(selectedGroup!!)
                    }) {
                        Text(group["name"] as String)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedGroup != null) {
                Text("Change Team")
                Spacer(modifier = Modifier.height(8.dp))

                DropdownMenu(
                    expanded = selectedTeam != null,
                    onDismissRequest = { selectedTeam = null }
                ) {
                    teams.forEach { team ->
                        DropdownMenuItem(onClick = {
                            selectedTeam = team["uid"] as String
                        }) {
                            Text(team["name"] as String)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    if (selectedGroup != null && selectedTeam != null) {
                        db.collection("users").document(studentId)
                            .update(mapOf("group" to selectedGroup, "team" to selectedTeam))
                            .addOnSuccessListener {
                                student = student!!.toMutableMap().apply {
                                    put("group", selectedGroup!!)
                                    put("team", selectedTeam!!)
                                }
                            }
                            .addOnFailureListener { e ->
                                errorMessage = "Error updating student: $e"
                            }
                    }
                }) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.popBackStack() }) {
                Text("Back")
            }
        } else {
            Text("Loading student details...")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
