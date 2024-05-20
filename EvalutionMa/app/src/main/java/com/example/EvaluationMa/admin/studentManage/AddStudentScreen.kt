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
    var availableStudents by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedStudentIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("users").whereEqualTo("role", "student").get()
            .addOnSuccessListener { result ->
                val allStudents = result.documents.mapNotNull { document ->
                    val data = document.data
                    data?.put("uid", document.id)
                    data
                }

                db.collection("groups").get()
                    .addOnSuccessListener { groupResult ->
                        val assignedStudentIds = mutableSetOf<String>()
                        groupResult.documents.forEach { groupDocument ->
                            db.collection("groups").document(groupDocument.id).collection("teams").get()
                                .addOnSuccessListener { teamResult ->
                                    teamResult.documents.forEach { teamDocument ->
                                        db.collection("groups").document(groupDocument.id).collection("teams").document(teamDocument.id).collection("students").get()
                                            .addOnSuccessListener { studentResult ->
                                                studentResult.documents.forEach { studentDocument ->
                                                    assignedStudentIds.add(studentDocument.id)
                                                }

                                                // 筛选未分配的学生
                                                availableStudents = allStudents.filter { student ->
                                                    !assignedStudentIds.contains(student["uid"])
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                errorMessage = "Error fetching assigned students: $e"
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    errorMessage = "Error fetching teams: $e"
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        errorMessage = "Error fetching groups: $e"
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

        if (availableStudents.isNotEmpty()) {
            LazyColumn {
                items(availableStudents) { student ->
                    StudentSelectItem(
                        student = student,
                        isSelected = selectedStudentIds.contains(student["uid"] as String),
                        onSelect = { studentId ->
                            selectedStudentIds = if (selectedStudentIds.contains(studentId)) {
                                selectedStudentIds - studentId
                            } else {
                                selectedStudentIds + studentId
                            }
                        }
                    )
                }
            }
        } else {
            Text("No students available.")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            selectedStudentIds.forEach { studentId ->
                db.collection("groups").document(groupId).collection("teams").document(teamId).collection("students").document(studentId).set(mapOf("uid" to studentId))
                    .addOnSuccessListener {
                        db.collection("users").document(studentId).update(mapOf("group" to groupId, "team" to teamId))
                            .addOnFailureListener { e ->
                                errorMessage = "Error updating student: $e"
                            }
                    }
                    .addOnFailureListener { e ->
                        errorMessage = "Error adding student: $e"
                    }
            }
            navController.popBackStack()
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
    isSelected: Boolean,
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
        Button(
            onClick = { onSelect(studentId) },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isSelected) Color.Green else Color.White
            )
        ) {
            Text(if (isSelected) "Selected" else "Select")
        }
    }
}
