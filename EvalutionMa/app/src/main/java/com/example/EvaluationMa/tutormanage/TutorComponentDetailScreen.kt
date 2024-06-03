package com.example.EvaluationMa.tutormanage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import com.example.EvaluationMa.comanage.Skill
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun TutorComponentDetailScreen(navController: NavController, componentId: String, groupId: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var skills: List<Skill> by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // 获取技能
        loadSkills(db, componentId, onSuccess = { result ->
            skills = result
        }, onFailure = { e ->
            errorMessage = "Error fetching skills: $e"
        })
    }

    if (errorMessage.isNotEmpty()) {
        Text(errorMessage, color = Color.Red, modifier = Modifier.padding(16.dp))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text("Component Details", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                // 导航时传递 componentId 和 groupId
                navController.navigate("tutorAnnouncement/$componentId/$groupId")
            }) {
                Text("Go to Announcements")
            }

            // 显示技能列表
            Text("Skills", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            skills.forEach { skill ->
                SkillItem(skill)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 添加学生成绩按钮
            Button(onClick = {
                navController.navigate("team_list/$groupId/$componentId")
            }) {
                Text("Add Student Scores")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 查看考试成绩按钮
            Button(onClick = {
                navController.navigate("steam_list/$componentId/$groupId")
            }) {
                Text("View Exam Scores")
            }
        }
    }
}

@Composable
fun SkillItem(skill: Skill) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(skill.title, style = MaterialTheme.typography.body1)
            Text(skill.description, style = MaterialTheme.typography.body2)
            Text(skill.link, style = MaterialTheme.typography.body2, color = Color.Blue)
        }
    }
}

fun loadSkills(
    db: FirebaseFirestore,
    componentId: String,
    onSuccess: (List<Skill>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    db.collection("components").document(componentId).collection("skills").get()
        .addOnSuccessListener { result ->
            val skills = result.documents.mapNotNull { document ->
                document.toObject(Skill::class.java)?.copy(id = document.id)
            }
            onSuccess(skills)
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}