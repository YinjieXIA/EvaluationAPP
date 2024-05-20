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
fun AddStudentScreen(navController: NavController, groupId: String, teamId: String) {
    val db = FirebaseFirestore.getInstance()
    var students by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedStudentId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("users").whereEqualTo("role", "student").whereEqualTo("group", null).get()
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
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add Student to Team", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (students.isNotEmpty()) {
            LazyColumn {
                items(students) { student ->
                    StudentSelectItem(student, onSelect = { studentId ->
                        selectedStudentId = studentId
                    })
                }
            }
        } else {
            Text("No students available.")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (selectedStudentId != null) {
                db.collection("groups").document(groupId).collection("teams").document(teamId).collection("students").document(selectedStudentId!!).set(mapOf("uid" to selectedStudentId))
                    .addOnSuccessListener {
                        db.collection("users").document(selectedStudentId!!).update(mapOf("group" to groupId, "team" to teamId))
                            .addOnSuccessListener {
                                navController.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                errorMessage = "Error updating student: $e"
                            }
                    }
                    .addOnFailureListener { e ->
                        errorMessage = "Error adding student: $e"
                    }
            }
        }) {
            Text("Add Student")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun StudentSelectItem(
    student: Map<String, Any>,
    onSelect: (String) -> Unit
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
        Text(studentName)
        Button(onClick = { onSelect(studentId) }) {
            Text("Select")
        }
    }
}
