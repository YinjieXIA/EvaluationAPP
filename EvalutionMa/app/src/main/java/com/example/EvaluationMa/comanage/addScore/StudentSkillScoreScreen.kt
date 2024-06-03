package com.example.EvaluationMa.comanage.addScore

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

val scoreMap = mapOf(
    "Non Acquis" to 0.0,
    "Loin" to 7.0,
    "Proche" to 10.0,
    "Très proche" to 13.0,
    "Attendu" to 16.0,
    "Au-delà" to 20.0
)

val gradeOptions = listOf("Non Acquis", "Loin", "Proche", "Très proche", "Attendu", "Au-delà")
@Composable
fun StudentSkillScoreScreen(navController: NavController, groupId: String, teamId: String, componentId: String, skillId: String) {
    val db = FirebaseFirestore.getInstance()
    var students by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var studentScores by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    LaunchedEffect(teamId) {
        db.collection("groups").document(groupId)
            .collection("teams").document(teamId)
            .collection("students")
            .get()
            .addOnSuccessListener { result ->
                val studentIds = result.documents.mapNotNull { document -> document.id }

                // 初始空列表
                val loadedStudents = mutableListOf<Map<String, Any>>()

                // 异步加载每个学生的名字
                studentIds.forEach { studentId ->
                    fetchStudentName(studentId, onSuccess = { userData ->
                        // 组合学生信息并更新状态
                        val studentData = mapOf(
                            "uid" to studentId,
                            "firstName" to userData["firstName"],
                            "lastName" to userData["lastName"]
                        )
                        loadedStudents.add(studentData as Map<String, Any>)
                        students = loadedStudents.toList()
                    }, onFailure = { e ->
                        errorMessage = "Error fetching student name: $e"
                    })
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching students: $e"
                Log.e("StudentSkillScoreScreen", errorMessage)
            }
    }

    fun saveScores() {
        students.forEach { student ->
            val studentId = student["uid"] as String
            val grade = studentScores[studentId] ?: ""
            val scoreValue = scoreMap[grade]
            if (scoreValue != null) {
                saveStudentScore(groupId, teamId, studentId, skillId, componentId, scoreValue, {
                    successMessage = "Scores added successfully"
                    errorMessage = ""
                }, { e ->
                    errorMessage = "Error adding score for $studentId: $e"
                    successMessage = ""
                })
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Add Scores for Skill", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (students.isNotEmpty()) {
            LazyColumn {
                items(students) { student ->
                    val studentId = student["uid"] as String
                    val studentName = "${student["firstName"]} ${student["lastName"]}"
                    var expanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(studentName, modifier = Modifier.weight(1f))
                        Box {
                            TextButton(onClick = { expanded = !expanded }) {
                                Text(studentScores[studentId] ?: "Select Grade")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                gradeOptions.forEach { grade ->
                                    DropdownMenuItem(onClick = {
                                        studentScores = studentScores.toMutableMap().apply { this[studentId] = grade }
                                        expanded = false
                                    }) {
                                        Text(grade)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                saveScores()
            }) {
                Text("Save Scores")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (successMessage.isNotEmpty()) {
                Text(successMessage, color = Color.Green, modifier = Modifier.padding(top = 8.dp))
            }
        } else {
            Text("No students found.")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

fun saveStudentScore(
    groupId: String,
    teamId: String,
    studentId: String,
    skillId: String,
    componentId: String,
    score: Double,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val scoreData = mapOf(
        "score" to score,
        "timestamp" to System.currentTimeMillis(),
        "skillId" to skillId,
        "componentId" to componentId
    )
    db.collection("groups").document(groupId)
        .collection("teams").document(teamId)
        .collection("students").document(studentId)
        .collection("studentScores")
        .add(scoreData) // 使用 add 方法插入新文档
        .addOnSuccessListener {
            onSuccess()
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}

fun fetchStudentName(studentId: String, onSuccess: (Map<String, Any>) -> Unit, onFailure: (Exception) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(studentId).get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                onSuccess(document.data ?: emptyMap())
            } else {
                onFailure(Exception("No such student found"))
            }
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}