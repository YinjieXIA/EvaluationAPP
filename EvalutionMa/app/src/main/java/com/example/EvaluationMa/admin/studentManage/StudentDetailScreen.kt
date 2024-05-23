package com.example.EvaluationMa.admin.studentManage

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun StudentDetailScreen(navController: NavController, studentId: String) {
    val db = FirebaseFirestore.getInstance()
    var student by remember { mutableStateOf<Map<String, Any>?>(null) }
    var groups by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var teams by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var selectedTeam by remember { mutableStateOf<String?>(null) }
    var groupExpanded by remember { mutableStateOf(false) }
    var teamExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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

    LaunchedEffect(studentId) {
        db.collection("users").document(studentId).get()
            .addOnSuccessListener { document ->
                if (document.getString("role") == "student") {
                    student = document.data
                    val studentGroup = student?.get("group") as? String
                    val studentTeam = student?.get("team") as? String
                    selectedGroup = studentGroup
                    selectedTeam = studentTeam
                    studentGroup?.let { loadTeams(it) }
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
            val currentGroupName = groups.find { it["uid"] == student!!["group"] }?.get("name") ?: "None"
            val currentTeamName = teams.find { it["uid"] == student!!["team"] }?.get("name") ?: "None"
            Text("Current Group: $currentGroupName")
            Spacer(modifier = Modifier.height(16.dp))
            Text("Current Team: $currentTeamName")
            Spacer(modifier = Modifier.height(16.dp))

            Text("Change Group")
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = groupExpanded,
                onExpandedChange = {
                    groupExpanded = !groupExpanded
                }
            ) {
                TextField(
                    value = selectedGroup?.let { groupId ->
                        groups.find { it["uid"] == groupId }?.get("name") as String? ?: "Select Group"
                    } ?: "Select Group",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = groupExpanded
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = groupExpanded,
                    onDismissRequest = { groupExpanded = false }
                ) {
                    groups.forEach { group ->
                        DropdownMenuItem(onClick = {
                            selectedGroup = group["uid"] as String
                            loadTeams(selectedGroup!!)
                            groupExpanded = false
                        }) {
                            Text(group["name"] as String)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedGroup != null) {
                Text("Change Team")
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = teamExpanded,
                    onExpandedChange = {
                        teamExpanded = !teamExpanded
                    }
                ) {
                    TextField(
                        value = selectedTeam?.let { teamId ->
                            teams.find { it["uid"] == teamId }?.get("name") as String? ?: "Select Team"
                        } ?: "Select Team",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = teamExpanded
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = teamExpanded,
                        onDismissRequest = { teamExpanded = false }
                    ) {
                        teams.forEach { team ->
                            DropdownMenuItem(onClick = {
                                selectedTeam = team["uid"] as String
                                teamExpanded = false
                            }) {
                                Text(team["name"] as String)
                            }
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
                                navController.navigate("student_management") // 跳转回 StudentManagementScreen
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
