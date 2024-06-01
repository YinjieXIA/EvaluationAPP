package com.example.EvaluationMa.comanage

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
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



data class StudentScore(val studentId: String = "", val skillId: String = "", val score: Int = 0, val componentId: String = "", val timestamp: Long = 0)
data class Team(val id: String = "", val name: String = "", val memberIds: List<String> = emptyList())

@Composable
fun StudentScoresScreen(navController: NavController, componentId: String, groupId: String?) {
    val db = FirebaseFirestore.getInstance()
    var students by remember { mutableStateOf<List<User>>(emptyList()) }
    var skills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var studentScores by remember { mutableStateOf<Map<String, Map<String, Int>>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf("") }
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

        // 获取技能列表
        db.collection("components").document(componentId).collection("skills").get()
            .addOnSuccessListener { result ->
                skills = result.documents.mapNotNull { document ->
                    document.toObject(Skill::class.java)?.copy(id = document.id)
                }
                Log.d("ComponentDetailScreen", "Fetched skills: $skills")
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching skills: $e"
                Log.e("ComponentDetailScreen", errorMessage)
            }

        // 获取学生列表
        val studentQuery = if (groupId != null) {
            db.collection("groups").document(groupId).collection("students")
        } else {
            db.collection("users").whereEqualTo("role", "student")
        }
        studentQuery.get()
            .addOnSuccessListener { result ->
                students = result.documents.mapNotNull { document ->
                    document.toObject(User::class.java)?.copy(uid = document.id)
                }
                Log.d("ComponentDetailScreen", "Fetched students: $students")
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching students: $e"
                Log.e("ComponentDetailScreen", errorMessage)
            }

        // 获取学生成绩
        db.collection("components").document(componentId).collection("studentScores").get()
            .addOnSuccessListener { result ->
                val scores = result.documents.mapNotNull { document ->
                    document.toObject(StudentScore::class.java)
                }.groupBy { it.studentId }
                    .mapValues { entry ->
                        entry.value.associateBy { it.skillId }.mapValues { it.value.score }
                    }
                studentScores = scores
                Log.d("ComponentDetailScreen", "Fetched student scores: $studentScores")
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching student scores: $e"
                Log.e("ComponentDetailScreen", errorMessage)
            }
    }

    if (errorMessage.isNotEmpty()) {
        Text(errorMessage, color = Color.Red, modifier = Modifier.padding(16.dp))
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Student Scores", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            if (role == "tutor" || role == "component_manager" || role == "module_manager") {
                skills.forEach { skill ->
                    Text("${skill.title} Scores", style = MaterialTheme.typography.h6)
                    Spacer(modifier = Modifier.height(8.dp))
                    SkillScoresTable(
                        students,
                        skill,
                        studentScores,
                        componentId,
                        db,
                        onScoreUpdate = { studentId, score ->
                            coroutineScope.launch {
                                val newScore = StudentScore(
                                    studentId,
                                    skill.id,
                                    score,
                                    componentId,
                                    System.currentTimeMillis()
                                )
                                db.collection("components").document(componentId)
                                    .collection("studentScores").add(newScore)
                                    .addOnSuccessListener {
                                        val updatedScores = studentScores.toMutableMap()
                                        val studentSkillScores =
                                            updatedScores[studentId]?.toMutableMap()
                                                ?: mutableMapOf()
                                        studentSkillScores[skill.id] = score
                                        updatedScores[studentId] = studentSkillScores
                                        studentScores = updatedScores
                                        Log.d(
                                            "ComponentDetailScreen",
                                            "Added score for student: $studentId, skill: ${skill.id}"
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        errorMessage = "Error adding score to component: $e"
                                        Log.e("ComponentDetailScreen", errorMessage)
                                    }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 总分表
                TotalScoresTable(students, skills, studentScores, db, componentId, navController)
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = Color.Red, modifier = Modifier.padding(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Au-delà = 20,\nAttendu = 16,\nTrès proche = 13,\nProche = 10,\nLoin = 7,\nNon Acquis = 0",
                style = MaterialTheme.typography.body1
            )

            // 调试信息
            Text(
                "Debug Info: Students: $students",
                style = MaterialTheme.typography.body2,
                color = Color.Gray
            )
            Text(
                "Debug Info: Skills: $skills",
                style = MaterialTheme.typography.body2,
                color = Color.Gray
            )
            Text(
                "Debug Info: StudentScores: $studentScores",
                style = MaterialTheme.typography.body2,
                color = Color.Gray
            )
        }
    }
}


@Composable
fun SkillScoresTable(
    students: List<User>,
    skill: Skill,
    studentScores: Map<String, Map<String, Int>>,
    componentId: String,
    db: FirebaseFirestore,
    onScoreUpdate: (String, Int) -> Unit
) {
    var scoreInputs by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var expandedStates by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    // 初始化分数输入状态
    LaunchedEffect(skill, studentScores) {
        val inputs = mutableMapOf<String, String>()
        val expanded = mutableMapOf<String, Boolean>()
        students.forEach { student ->
            val studentId = student.uid
            val score = studentScores[studentId]?.get(skill.id) ?: -1
            inputs[studentId] = when (score) {
                20 -> "Au-delà"
                16 -> "Attendu"
                13 -> "Très proche"
                10 -> "Proche"
                7 -> "Loin"
                -1 -> "-"
                else -> "Non Acquis"
            }
            expanded[studentId] = false
        }
        scoreInputs = inputs
        expandedStates = expanded
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Student", modifier = Modifier.weight(1f))
            Text(skill.title, modifier = Modifier.weight(1f))
        }

        students.forEach { student ->
            val studentId = student.uid
            var skillScore by remember { mutableStateOf(scoreInputs[studentId] ?: "-") }
            var expanded by remember { mutableStateOf(expandedStates[studentId] ?: false) }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(student.email, modifier = Modifier.weight(1f))
                Column(modifier = Modifier.weight(1f)) {
                    Text(skillScore, modifier = Modifier.clickable { expanded = !expanded })
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("Au-delà", "Attendu", "Très proche", "Proche", "Loin", "Non Acquis").forEach { grade ->
                            DropdownMenuItem(onClick = {
                                skillScore = grade
                                expanded = false
                                val score = when (grade) {
                                    "Au-delà" -> 20
                                    "Attendu" -> 16
                                    "Très proche" -> 13
                                    "Proche" -> 10
                                    "Loin" -> 7
                                    else -> 0
                                }
                                onScoreUpdate(studentId, score)
                            }) {
                                Text(grade)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TotalScoresTable(
    students: List<User>,
    skills: List<Skill>,
    studentScores: Map<String, Map<String, Int>>,
    db: FirebaseFirestore,
    componentId: String,
    navController: NavController
) {
    var totalScores by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // 初始化总分
    LaunchedEffect(studentScores) {
        val totals = mutableMapOf<String, String>()
        students.forEach { student ->
            val studentId = student.uid
            var totalScore: Double? = 0.0
            var allScoresAvailable = true
            skills.forEach { skill ->
                val score = studentScores[studentId]?.get(skill.id)?.toDouble()
                if (score == null) {
                    allScoresAvailable = false
                } else {
                    totalScore = totalScore!! + score * skill.weight.toDouble()
                }
            }
            if (allScoresAvailable && totalScore != null) {
                totals[studentId] = totalScore.toString()
            } else {
                totals[studentId] = "NULL"
            }
        }
        totalScores = totals
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Student", modifier = Modifier.weight(1f))
            Text("Total Score", modifier = Modifier.weight(1f))
            Text("Evaluate", modifier = Modifier.weight(1f))
        }

        students.forEach { student ->
            val studentId = student.uid
            val totalScore = totalScores[studentId] ?: "NULL"

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(student.email, modifier = Modifier.weight(1f))
                Text(totalScore, modifier = Modifier.weight(1f))
                IconButton(onClick = { navController.navigate("student_evaluation/${studentId}") }) {
                    Icon(imageVector = Icons.Default.Comment, contentDescription = "Evaluate")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 保存按钮
        Button(onClick = {
            students.forEach { student ->
                val studentId = student.uid
                val totalScore = totalScores[studentId]

                if (totalScore != "NULL") {
                    // 保存总分到数据库
                    db.collection("components").document(componentId)
                        .collection("totalScores").document(studentId).set(
                            mapOf("totalScore" to totalScore!!.toDouble(), "timestamp" to System.currentTimeMillis())
                        )

                    // 查询所有组，找到包含学生的团队，并将总分添加到团队中
                    db.collectionGroup("students").whereEqualTo("uid", studentId).get()
                        .addOnSuccessListener { result ->
                            result.documents.forEach { document ->
                                val teamPath = document.reference.parent.parent?.parent?.path // 获取team的路径
                                if (teamPath != null) {
                                    db.collection("$teamPath/totalScores").document(studentId).set(
                                        mapOf("totalScore" to totalScore!!.toDouble(), "timestamp" to System.currentTimeMillis())
                                    )
                                }
                            }
                        }
                }
            }
        }) {
            Text("Save")
        }
    }
}