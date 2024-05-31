package com.example.EvaluationMa.admin.studentManage

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.runtime.*
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
    var teams by remember { mutableStateOf<Map<String, List<Map<String, Any>>>>(emptyMap()) }
    var expandedTeams by remember { mutableStateOf<Set<String>>(emptySet()) }
    var students by remember { mutableStateOf<Map<String, List<Map<String, Any>>>>(emptyMap()) }
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
                teams = teams.toMutableMap().apply {
                    this[groupId] = result.documents.mapNotNull { document ->
                        val data = document.data
                        data?.put("uid", document.id)
                        data
                    }
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching teams: $e"
            }
    }

    fun loadStudents(groupId: String, teamId: String) {
        db.collection("groups").document(groupId).collection("teams").document(teamId).collection("students").get()
            .addOnSuccessListener { result ->
                students = students.toMutableMap().apply {
                    this[teamId] = result.documents.mapNotNull { document ->
                        val data = document.data
                        data?.put("uid", document.id)
                        data
                    }
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching students: $e"
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Group & Team Management", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(16.dp))

            if (groups.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(groups) { group ->
                        var showTeams by remember { mutableStateOf(false) }
                        Column {
                            GroupItem(
                                group = group,
                                onEdit = { groupId ->
                                    selectedGroupId = groupId
                                    expandedTeams = emptySet()
                                    loadTeams(groupId)
                                    showTeams = !showTeams
                                },
                                onDelete = { groupId ->
                                    db.collection("groups").document(groupId).delete()
                                        .addOnSuccessListener {
                                            groups = groups.filterNot { it["uid"] == groupId }
                                            if (selectedGroupId == groupId) {
                                                selectedGroupId = null
                                                expandedTeams = emptySet()
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
                                },
                                onAddTeam = { groupId ->
                                    navController.navigate("add_team/$groupId")
                                }
                            )
                            if (showTeams && selectedGroupId == group["uid"]) {
                                TeamsList(
                                    groupId = selectedGroupId!!,
                                    teams = teams[selectedGroupId!!] ?: emptyList(),
                                    expandedTeams = expandedTeams,
                                    onEdit = { teamId ->
                                        expandedTeams = expandedTeams.toMutableSet().apply {
                                            if (contains(teamId)) remove(teamId) else add(teamId)
                                        }
                                        loadStudents(selectedGroupId!!, teamId)
                                    },
                                    onDelete = { teamId ->
                                        db.collection("groups").document(selectedGroupId!!).collection("teams").document(teamId).delete()
                                            .addOnSuccessListener {
                                                teams = teams.toMutableMap().apply {
                                                    this[selectedGroupId!!] = this[selectedGroupId]!!.filterNot { it["uid"] == teamId }
                                                }
                                                expandedTeams = expandedTeams.toMutableSet().apply { remove(teamId) }
                                                students = students.toMutableMap().apply { remove(teamId) }
                                            }
                                            .addOnFailureListener { e ->
                                                errorMessage = "Error deleting team: $e"
                                            }
                                    },
                                    onAddStudent = { teamId ->
                                        navController.navigate("add_student/$selectedGroupId/$teamId")
                                    },
                                    students = students,
                                    onEditStudent = { studentId ->
                                        navController.navigate("student_detail/$studentId")
                                    },
                                    onDeleteStudent = { teamId, studentId ->
                                        db.collection("groups").document(selectedGroupId!!).collection("teams").document(teamId).collection("students").document(studentId).delete()
                                            .addOnSuccessListener {
                                                students = students.toMutableMap().apply {
                                                    this[teamId] = this[teamId]!!.filterNot { it["uid"] == studentId }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                errorMessage = "Error deleting student: $e"
                                            }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                Text("No groups found.")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(onClick = { navController.navigate("add_group") }) {
                Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Add Group", tint = Color.White)
            }
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
    onAssignTutor: (String) -> Unit,
    onAddTeam: (String) -> Unit
) {
    val groupId = group["uid"] as? String ?: ""
    val groupName = group["name"] as? String ?: ""
    val assignedClient = group["client"] as? String ?: "None"

    // 添加调试日志，打印出 components 的类型和内容
    val components = group["components"]
    Log.d("Debug", "group[\"components\"] type: ${components?.javaClass?.name}, value: $components")

    val componentList = if (components is List<*>) {
        components.filterIsInstance<Map<String, Any>>()
    } else {
        emptyList<Map<String, Any>>()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(groupName, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
            IconButton(onClick = { onAddTeam(groupId) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Team")
            }
            IconButton(onClick = { onEdit(groupId) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Group")
            }
            IconButton(onClick = { onDelete(groupId) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Group")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Assigned Client: $assignedClient", style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
            Row {
                IconButton(onClick = { onAssignClient(groupId) }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Assign Client")
                }
                IconButton(onClick = { onRemoveClient(groupId) }) {
                    Icon(Icons.Default.PersonRemove, contentDescription = "Remove Client")
                }
            }
        }
        componentList.forEach { component ->
            val componentId = component["componentId"] as? String ?: ""
            val tutor = component["tutor"] as? String ?: "None"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Component: $componentId", style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
                Text("Tutor: $tutor", style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
                IconButton(onClick = { onAssignTutor(groupId) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Tutor")
                }
                IconButton(onClick = { /* implement delete tutor logic */ }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Tutor")
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { onAssignComponent(groupId) }) {
                Text("Manage Components and Tutors")
            }
        }
    }
}

@Composable
fun TeamsList(
    groupId: String,
    teams: List<Map<String, Any>>,
    expandedTeams: Set<String>,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddStudent: (String) -> Unit,
    students: Map<String, List<Map<String, Any>>>,
    onEditStudent: (String) -> Unit,
    onDeleteStudent: (String, String) -> Unit
) {
    teams.forEach { team ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(team["name"] as? String ?: "", style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
                Row {
                    IconButton(onClick = { onAddStudent(team["uid"] as String) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Student")
                    }
                    IconButton(onClick = { onEdit(team["uid"] as String) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Team")
                    }
                    IconButton(onClick = { onDelete(team["uid"] as String) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Team")
                    }
                }
            }
            if (expandedTeams.contains(team["uid"] as String)) {
                students[team["uid"]]?.forEach { student ->
                    StudentItem(
                        student = student,
                        onEdit = onEditStudent,
                        onDelete = { onDeleteStudent(team["uid"] as String, it) }
                    )
                }
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(studentName, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
        IconButton(onClick = { onEdit(studentId) }) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Student")
        }
        IconButton(onClick = { onDelete(studentId) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Student")
        }
    }
}
