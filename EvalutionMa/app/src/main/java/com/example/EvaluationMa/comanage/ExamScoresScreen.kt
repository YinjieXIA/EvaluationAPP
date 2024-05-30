package com.example.EvaluationMa.comanage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
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
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ExamScoresScreen(navController: NavController, componentId: String, examName: String) {
    val db = FirebaseFirestore.getInstance()
    var students by remember { mutableStateOf<List<User>>(emptyList()) }
    var skills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var studentScores by remember { mutableStateOf<List<StudentScore>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // 获取学生列表
        db.collection("users").whereEqualTo("role", "student").get()
            .addOnSuccessListener { result ->
                students = result.documents.mapNotNull { document ->
                    document.toObject(User::class.java)?.copy(uid = document.id)
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching students: $e"
            }

        // 获取技能列表
        db.collection("components").document(componentId).collection("skills").get()
            .addOnSuccessListener { result ->
                skills = result.documents.mapNotNull { document ->
                    document.toObject(Skill::class.java)?.copy(id = document.id)
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching skills: $e"
            }

        // 获取学生成绩
        db.collection("components").document(componentId).collection("studentScores")
            .whereEqualTo("examName", examName).get()
            .addOnSuccessListener { result ->
                studentScores = result.documents.mapNotNull { document ->
                    document.toObject(StudentScore::class.java)
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching student scores: $e"
            }
    }

    if (errorMessage.isNotEmpty()) {
        Text(errorMessage, color = Color.Red, modifier = Modifier.padding(16.dp))
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Exam Scores: $examName", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        ExamScoresTable(students, skills, studentScores, componentId, examName, db, navController)
    }
}

@Composable
fun ExamScoresTable(
    students: List<User>,
    skills: List<Skill>,
    studentScores: List<StudentScore>,
    componentId: String,
    examName: String,
    db: FirebaseFirestore,
    navController: NavController
) {
    var totalScores by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var editMode by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var scoreInputs by remember { mutableStateOf<Map<String, MutableMap<String, String>>>(emptyMap()) }

    // 初始化总分和编辑状态
    LaunchedEffect(studentScores) {
        val totals = mutableMapOf<String, Int>()
        val edits = mutableMapOf<String, Boolean>()
        val inputs = mutableMapOf<String, MutableMap<String, String>>()

        students.forEach { student ->
            val studentId = student.uid
            val skillScores = studentScores.filter { it.studentId == studentId }
            val skillInputs = mutableMapOf<String, String>()
            var totalScore = 0

            skills.forEach { skill ->
                val score = skillScores.find { it.skillId == skill.id }?.score ?: 0
                skillInputs[skill.id] = score.toString()
                totalScore += score
            }

            inputs[studentId] = skillInputs
            totals[studentId] = totalScore
            edits[studentId] = false
        }

        scoreInputs = inputs
        totalScores = totals
        editMode = edits
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Student", modifier = Modifier.weight(1f))
            skills.forEach { skill ->
                Text(skill.title, modifier = Modifier.weight(1f))
            }
            Text("Total", modifier = Modifier.weight(1f))
            Text("Action", modifier = Modifier.weight(1f))
        }

        students.forEach { student ->
            val studentId = student.uid
            val skillInputs = scoreInputs[studentId] ?: mutableMapOf()
            val totalScore = totalScores[studentId] ?: 0

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(student.email, modifier = Modifier.weight(1f))

                skills.forEach { skill ->
                    val skillId = skill.id
                    if (editMode[studentId] == true) {
                        var skillScore by remember { mutableStateOf(skillInputs[skillId] ?: "") }
                        TextField(
                            value = skillScore,
                            onValueChange = { newValue ->
                                skillScore = newValue
                                val newScore = newValue.toIntOrNull() ?: 0
                                skillInputs[skillId] = newValue

                                // 重新计算总分
                                val newTotalScore = skillInputs.values.sumOf { it.toIntOrNull() ?: 0 }
                                totalScores = totalScores.toMutableMap().apply { this[studentId] = newTotalScore }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(skillInputs[skillId] ?: "0", modifier = Modifier.weight(1f))
                    }
                }

                Text(totalScore.toString(), modifier = Modifier.weight(1f))

                if (editMode[studentId] == true) {
                    IconButton(onClick = {
                        // 更新数据库中的分数
                        skills.forEach { skill ->
                            val skillId = skill.id
                            val scoreText = skillInputs[skillId] ?: "0"
                            val score = scoreText.toIntOrNull() ?: 0

                            db.collection("components").document(componentId).collection("studentScores")
                                .whereEqualTo("studentId", studentId)
                                .whereEqualTo("skillId", skillId)
                                .whereEqualTo("examName", examName)
                                .get()
                                .addOnSuccessListener { result ->
                                    if (result.isEmpty) {
                                        // 如果没有记录，添加新记录
                                        db.collection("components").document(componentId).collection("studentScores").add(
                                            StudentScore(studentId, skillId, score, componentId, System.currentTimeMillis(), examName)
                                        )
                                    } else {
                                        // 如果有记录，更新现有记录
                                        for (document in result.documents) {
                                            db.collection("components").document(componentId).collection("studentScores").document(document.id)
                                                .set(StudentScore(studentId, skillId, score, componentId, System.currentTimeMillis(), examName))
                                        }
                                    }
                                }
                        }

                        // 更新editMode状态
                        editMode = editMode.toMutableMap().apply { this[studentId] = false }
                    }) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                    }
                } else {
                    IconButton(onClick = {
                        // 更新editMode状态
                        editMode = editMode.toMutableMap().apply { this[studentId] = true }
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Update")
                    }

                    // 新增评价按钮
                    IconButton(onClick = {
                        navController.navigate("student_evaluation/$componentId/$examName/$studentId")
                    }) {
                        Icon(imageVector = Icons.Default.Comment, contentDescription = "Evaluate")
                    }
                }
            }
        }
    }
}