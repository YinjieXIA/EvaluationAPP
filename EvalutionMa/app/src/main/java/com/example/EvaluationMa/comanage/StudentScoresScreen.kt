package com.example.EvaluationMa.comanage

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch


data class StudentScore(val studentId: String = "", val skillId: String = "", val score: Int = 0, val componentId: String = "", val timestamp: Long = 0, val examName: String = "")
data class Team(val id: String = "", val name: String = "", val memberIds: List<String> = emptyList())

@Composable
fun StudentScoresScreen(navController: NavController, componentId: String) {
    val db = FirebaseFirestore.getInstance()
    var students by remember { mutableStateOf<List<User>>(emptyList()) }
    var skills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var studentScores by remember { mutableStateOf<List<StudentScore>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    var examName by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var role by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // 获取当前用户角色
        currentUser?.let { user ->
            val userDocPath = "users/${user.uid}"
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    role = document.getString("role")
                    Log.d("ComponentDetailScreen", "User role: $role")
                }
                .addOnFailureListener { exception ->
                    errorMessage = "Error getting user role from $userDocPath: $exception"
                    Log.e("ComponentDetailScreen", errorMessage)
                }
        }

        // 获取学生列表
        val studentsPath = "users"
        db.collection("users").whereEqualTo("role", "student").get()
            .addOnSuccessListener { result ->
                students = result.documents.mapNotNull { document ->
                    document.toObject(User::class.java)?.copy(uid = document.id)
                }
                Log.d("ComponentDetailScreen", "Fetched students: $students")
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching students from $studentsPath: $e"
                Log.e("ComponentDetailScreen", errorMessage)
            }

        // 获取技能列表
        val skillsPath = "components/$componentId/skills"
        db.collection("components").document(componentId).collection("skills").get()
            .addOnSuccessListener { result ->
                skills = result.documents.mapNotNull { document ->
                    document.toObject(Skill::class.java)?.copy(id = document.id)
                }
                Log.d("ComponentDetailScreen", "Fetched skills: $skills")
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching skills from $skillsPath: $e"
                Log.e("ComponentDetailScreen", errorMessage)
            }

        // 获取学生成绩
//        val studentScoresPath = "components/$componentId/studentScores"
//        db.collection("components").document(componentId).collection("studentScores").get()
//            .addOnSuccessListener { result ->
//                studentScores = result.documents.mapNotNull { document ->
//                    document.toObject(StudentScore::class.java)
//                }
//                Log.d("ComponentDetailScreen", "Fetched student scores: $studentScores")
//            }
//            .addOnFailureListener { e ->
//                errorMessage = "Error fetching student scores from $studentScoresPath: $e"
//                Log.e("ComponentDetailScreen", errorMessage)
//            }
    }

    if (errorMessage.isNotEmpty()) {
        Text(errorMessage, color = Color.Red, modifier = Modifier.padding(16.dp))
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Student Scores", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = examName,
            onValueChange = { examName = it },
            label = { Text("Exam Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (role == "tutor" || role == "component_manager" || role == "module_manager") {
            Text("Student Scores", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            StudentScoresTable(
                students,
                skills,
                studentScores,
                componentId,
                examName,
                db,
                onScoreUpdate = { studentId, skillId, score ->
                    coroutineScope.launch {
                        val newScore = StudentScore(
                            studentId,
                            skillId,
                            score,
                            componentId,
                            System.currentTimeMillis(),
                            examName
                        )
                        db.collection("components").document(componentId)
                            .collection("studentScores").add(newScore)
                            .addOnSuccessListener {
                                studentScores = studentScores + newScore
                                // 查询学生所在的团队，并将成绩添加到团队中
                                db.collectionGroup("students").whereEqualTo("uid", studentId).get()
                                    .addOnSuccessListener { result ->
                                        result.documents.forEach { document ->
                                            val teamPath =
                                                document.reference.parent.parent?.parent?.path // 获取team的路径
                                            if (teamPath != null) {
                                                db.collection("$teamPath/studentScores").add(
                                                    StudentScore(
                                                        studentId = studentId,
                                                        skillId = skillId,
                                                        score = score,
                                                        componentId = componentId,
                                                        timestamp = System.currentTimeMillis(),
                                                        examName = examName
                                                    )
                                                ).addOnSuccessListener {
                                                    Log.d(
                                                        "StudentScoresScreen",
                                                        "Score added to team at $teamPath"
                                                    )
                                                }.addOnFailureListener { e ->
                                                    Log.e(
                                                        "StudentScoresScreen",
                                                        "Error adding score to team at $teamPath: $e"
                                                    )
                                                }
                                            }
                                        }
                                    }.addOnFailureListener { e ->
                                        Log.e(
                                            "StudentScoresScreen",
                                            "Error fetching teams for student: $e"
                                        )
                                    }
                            }
                            .addOnFailureListener { e ->
                                errorMessage = "Error adding score to component: $e"
                                Log.e("StudentScoresScreen", errorMessage)
                            }
                    }

                })
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
fun StudentScoresTable(
    students: List<User>,
    skills: List<Skill>,
    studentScores: List<StudentScore>,
    componentId: String,
    examName: String,
    db: FirebaseFirestore,
    onScoreUpdate: (String, String, Int) -> Unit
) {
    var scoreInputs by remember { mutableStateOf<Map<String, MutableMap<String, String>>>(emptyMap()) }
    var totalScores by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // 初始化分数输入状态
    LaunchedEffect(studentScores) {
        val inputs = mutableMapOf<String, MutableMap<String, String>>()
        val totals = mutableMapOf<String, Int>()
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
        }
        scoreInputs = inputs
        totalScores = totals
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Student", modifier = Modifier.weight(1f))
            skills.forEach { skill ->
                Text(skill.title, modifier = Modifier.weight(1f))
            }
            Text("Total", modifier = Modifier.weight(1f))
        }

        students.forEach { student ->
            val studentId = student.uid
            val skillInputs = scoreInputs[studentId] ?: mutableMapOf()
            var totalScore by remember { mutableStateOf(totalScores[studentId] ?: 0) }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(student.email, modifier = Modifier.weight(1f))

                skills.forEach { skill ->
                    val skillId = skill.id
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
                            totalScore = newTotalScore
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(totalScore.toString(), modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 提交按钮
        Button(onClick = {
            students.forEach { student ->
                val studentId = student.uid
                val skillInputs = scoreInputs[studentId] ?: mutableMapOf()

                skills.forEach { skill ->
                    val skillId = skill.id
                    val scoreText = skillInputs[skillId] ?: "0"
                    val score = scoreText.toIntOrNull() ?: 0

                    // 更新数据库中的分数
                    onScoreUpdate(studentId, skillId, score)

                    // 查询所有组，找到包含学生的团队，将成绩添加到团队中
                    db.collection("groups").get()
                        .addOnSuccessListener { groups ->
                            groups.documents.forEach { group ->
                                db.collection("groups").document(group.id).collection("teams").get()
                                    .addOnSuccessListener { teams ->
                                        teams.documents.forEach { team ->
                                            db.collection("groups").document(group.id).collection("teams").document(team.id).collection("students").document(studentId).get()
                                                .addOnSuccessListener { studentDoc ->
                                                    if (studentDoc.exists()) {
                                                        val newScore = StudentScore(studentId, skillId, score, componentId, System.currentTimeMillis(), examName)
                                                        db.collection("groups").document(group.id).collection("teams").document(team.id).collection("studentScores").add(newScore)
                                                            .addOnSuccessListener {
                                                                Log.d("StudentScoresTable", "Score added to team ${team.id}")
                                                            }
                                                            .addOnFailureListener { e ->
                                                                Log.e("StudentScoresTable", "Error adding score to team: $e")
                                                            }
                                                    }
                                                }
                                        }
                                    }
                            }
                        }
                }
            }
        }) {
            Text("Submit Scores")
        }
    }
}