package com.example.EvaluationMa.comanage.ScoreList

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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.EvaluationMa.comanage.Skill
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.round

val scoreToGradeMap = mapOf(
    0.0 to "Non Acquis",
    7.0 to "Loin",
    10.0 to "Proche",
    13.0 to "Très proche",
    16.0 to "Attendu",
    20.0 to "Au-delà"
)

fun getGradeFromScore(score: Double): String {
    return scoreToGradeMap[score] ?: "Unknown"
}

fun calculateTotalScore(scores: Map<String, Double>, skills: List<Skill>): Double {
    val totalScore = skills.sumOf { skill ->
        val score = scores[skill.id] ?: 0.0
        score * skill.weight
    }
    return round(totalScore * 100) / 100  // 保留两位小数
}

fun updateTotalScores(
    groupId: String,
    teamId: String,
    students: List<Map<String, Any>>,
    componentId: String,
    skills: List<Skill>,
    studentScores: Map<String, Map<String, Double>>,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    students.forEach { student ->
        val studentId = student["uid"] as String
        val scores = studentScores[studentId] ?: emptyMap()
        val totalScore = calculateTotalScore(scores, skills)
        val totalScoreData = mapOf(
            "totalScore" to totalScore,
            "componentId" to componentId
        )

        val studentTotalScoresRef = db.collection("groups").document(groupId)
            .collection("teams").document(teamId)
            .collection("students").document(studentId)
            .collection("totalScores")

        studentTotalScoresRef.whereEqualTo("componentId", componentId).get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    // Add new total score
                    studentTotalScoresRef.add(totalScoreData)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onFailure(e) }
                } else {
                    // Update existing total score
                    result.documents.forEach { document ->
                        studentTotalScoresRef.document(document.id).update(totalScoreData)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e -> onFailure(e) }
                    }
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }
}

@Composable
fun TeamStudentScoresScreen(navController: NavController, componentId: String, groupId: String, teamId: String) {
    val db = FirebaseFirestore.getInstance()
    var students by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var skills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var studentScores by remember { mutableStateOf<Map<String, Map<String, Double>>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    LaunchedEffect(teamId) {
        db.collection("groups").document(groupId)
            .collection("teams").document(teamId)
            .collection("students")
            .get()
            .addOnSuccessListener { result ->
                students = result.documents.mapNotNull { document ->
                    document.data?.apply { put("uid", document.id) }
                }

                // Fetch scores for each student
                students.forEach { student ->
                    val studentId = student["uid"] as String
                    db.collection("groups").document(groupId)
                        .collection("teams").document(teamId)
                        .collection("students").document(studentId)
                        .collection("studentScores")
                        .whereEqualTo("componentId", componentId)
                        .get()
                        .addOnSuccessListener { scoreResult ->
                            val scores = scoreResult.documents.mapNotNull { document ->
                                val skillId = document.getString("skillId") ?: return@mapNotNull null
                                val score = document.getDouble("score") ?: return@mapNotNull null
                                skillId to score
                            }.toMap()
                            studentScores = studentScores.toMutableMap().apply {
                                this[studentId] = scores
                            }
                        }
                        .addOnFailureListener { e ->
                            errorMessage = "Error fetching student scores: $e"
                        }
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching students: $e"
            }

        db.collection("components").document(componentId).collection("skills").get()
            .addOnSuccessListener { result ->
                skills = result.documents.mapNotNull { document ->
                    document.toObject(Skill::class.java)?.copy(id = document.id)
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching skills: $e"
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Student Scores", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (students.isNotEmpty() && skills.isNotEmpty()) {
            LazyColumn {
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Student Name", modifier = Modifier.weight(1f))
                        skills.forEach { skill ->
                            Text(skill.title, modifier = Modifier.weight(1f))
                        }
                        Text("SUM", modifier = Modifier.weight(1f))
                    }
                }
                items(students) { student ->
                    val studentId = student["uid"] as String
                    val studentName = "${student["firstName"]} ${student["lastName"]}"
                    val scores = studentScores[studentId] ?: emptyMap()
                    val totalScore = calculateTotalScore(scores, skills)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(studentName, modifier = Modifier.weight(1f))
                        skills.forEach { skill ->
                            val score = scores[skill.id] ?: 0.0
                            val grade = getGradeFromScore(score)
                            Text(grade, modifier = Modifier.weight(1f))
                        }
                        Text(String.format("%.2f", totalScore), modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                updateTotalScores(groupId, teamId, students, componentId, skills, studentScores, {
                    successMessage = "Total scores updated successfully"
                    errorMessage = ""
                }, { e ->
                    errorMessage = "Error updating total scores: $e"
                    successMessage = ""
                })
            }) {
                Text("Confirm Total Scores")
            }

            if (successMessage.isNotEmpty()) {
                Text(successMessage, color = Color.Green, modifier = Modifier.padding(top = 8.dp))
            }
        } else {
            Text("No student scores found.")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}