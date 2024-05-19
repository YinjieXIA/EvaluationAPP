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
fun StudentManagementScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var students by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("students").get()
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
        Text("Student Management", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (students.isNotEmpty()) {
            LazyColumn {
                items(students) { student ->
                    StudentItem(student, onEdit = { studentId ->
                        navController.navigate("student_detail/$studentId")
                    })
                }
            }
        } else {
            Text("No students found.")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }

        Button(onClick = { navController.navigate("add_student") }) {
            Text("Add Student")
        }
    }
}

@Composable
fun StudentItem(
    student: Map<String, Any>,
    onEdit: (String) -> Unit
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
        Button(onClick = { onEdit(studentId) }) {
            Text("Edit")
        }
    }
}
