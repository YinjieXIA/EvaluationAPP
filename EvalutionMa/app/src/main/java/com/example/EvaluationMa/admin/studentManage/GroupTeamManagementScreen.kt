package com.example.EvaluationMa.admin.studentManage

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
fun GroupTeamManagementScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var groups by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var teams by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var students by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
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

    fun loadStudents(groupId: String, teamId: String) {
        db.collection("groups").document(groupId).collection("teams").document(teamId).collection("students").get()
            .addOnSuccessListener { result ->
                students = result.documents.mapNotNull { document ->
                    val data = document.data
                    data?.put("uid", document.id)
                    data
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching students: $e"
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Group & Team Management", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (groups.isNotEmpty()) {
            LazyColumn {
                items(groups) { group ->
                    GroupItem(
                        group = group,
                        onEdit = { groupId ->
                            selectedGroupId = groupId
                            teams = emptyList()
                            students = emptyList()
                            loadTeams(groupId)
                        },
                        onDelete = { groupId ->
                            db.collection("groups").document(groupId).delete()
                                .addOnSuccessListener {
                                    groups = groups.filterNot { it["uid"] == groupId }
                                    if (selectedGroupId == groupId) {
                                        selectedGroupId = null
                                        teams = emptyList()
                                        students = emptyList()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    errorMessage = "Error deleting group: $e"
                                }
                        },
                        onAssignClient = { groupId ->
                            navController.navigate("assign_client/$groupId")
                        },
                        onRemoveClient = { groupId ->
                            db.collection("groups").document(groupId).update("assignedClient", null)
                                .addOnSuccessListener {
                                    errorMessage = "Client removed successfully"
                                }
                                .addOnFailureListener { e ->
                                    errorMessage = "Error removing client: $e"
                                }
                        },
                        onAssignComponent = { groupId ->
                            navController.navigate("assign_component/$groupId")
                        },
                        onAssignTutor = { groupId ->
                            navController.navigate("assign_tutor/$groupId")
                        }
                    )
                }
            }
        } else {
            Text("No groups found.")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedGroupId != null) {
            Text("Teams", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))

            if (teams.isNotEmpty()) {
                LazyColumn {
                    items(teams) { team ->
                        TeamItem(
                            team = team,
                            onEdit = { teamId ->
                                loadStudents(selectedGroupId!!, teamId)
                            },
                            onDelete = { teamId ->
                                db.collection("groups").document(selectedGroupId!!).collection("teams").document(teamId).delete()
                                    .addOnSuccessListener {
                                        teams = teams.filterNot { it["uid"] == teamId }
                                    }
                                    .addOnFailureListener { e ->
                                        errorMessage = "Error deleting team: $e"
                                    }
                            }
                        )
                    }
                }
            } else {
                Text("No teams found.")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.navigate("add_team/$selectedGroupId") }) {
                Text("Add Team")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("add_group") }) {
            Text("Add Group")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun GroupItem(
    group: Map<String, Any>,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAssignClient: (String) -> Unit,
    onRemoveClient: (String) -> Unit,
    onAssignComponent: (String) -> Unit,
    onAssignTutor: (String) -> Unit
) {
    val groupId = group["uid"] as? String ?: ""
    val groupName = group["name"] as? String ?: ""
    val assignedClient = group["client"] as? String ?: "No client assigned"
    val assignedTutors = group["componentTutors"] as? Map<String, List<String>> ?: emptyMap()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(groupName, style = MaterialTheme.typography.body1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { onEdit(groupId) }, modifier = Modifier.weight(1f)) {
                Text("Edit")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onAssignComponent(groupId) }, modifier = Modifier.weight(1f)) {
                Text("Assign Component")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onDelete(groupId) }, modifier = Modifier.weight(1f)) {
                Text("Delete")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Assigned Client: $assignedClient", style = MaterialTheme.typography.body2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { onAssignClient(groupId) }, modifier = Modifier.weight(1f)) {
                Text("Assign Client")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onRemoveClient(groupId) }, modifier = Modifier.weight(1f)) {
                Text("Remove Client")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Assigned Tutors:", style = MaterialTheme.typography.body2)
        assignedTutors.forEach { (componentId, tutors) ->
            Text("Component $componentId: ${tutors.joinToString(", ")}", style = MaterialTheme.typography.body2)
        }
        Button(onClick = { onAssignTutor(groupId) }, modifier = Modifier.weight(1f)) {
            Text("Assign Tutor")
        }
    }
}

@Composable
fun TeamItem(
    team: Map<String, Any>,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val teamId = team["uid"] as? String ?: ""
    val teamName = team["name"] as? String ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(teamName, style = MaterialTheme.typography.body1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { onEdit(teamId) }, modifier = Modifier.weight(1f)) {
                Text("Edit")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onDelete(teamId) }, modifier = Modifier.weight(1f)) {
                Text("Delete")
            }
        }
    }
}

@Composable
fun StudentItem(
    student: Map<String, Any>,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val studentId = student["uid"] as? String ?: ""
    val studentName = "${student["firstName"]} ${student["lastName"]}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(studentName, style = MaterialTheme.typography.body1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { onEdit(studentId) }, modifier = Modifier.weight(1f)) {
                Text("Edit")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onDelete(studentId) }, modifier = Modifier.weight(1f)) {
                Text("Delete")
            }
        }
    }
}
