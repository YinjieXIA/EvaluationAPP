package com.example.EvaluationMa.comanage

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

data class Announcement(val id: String = "", val title: String = "", val content: String = "", val timestamp: Long = 0)

@Composable
fun ComponentDetailScreen(navController: NavController, componentId: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var role by remember { mutableStateOf<String?>(null) }
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var skills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var showPostAnnouncement by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 获取当前用户角色
        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    role = document.getString("role")
                }
                .addOnFailureListener { exception ->
                    errorMessage = "Error getting user role: $exception"
                }
        }

        // 获取公告
        loadAnnouncements(db, componentId, onSuccess = { result ->
            announcements = result
        }, onFailure = { e ->
            errorMessage = "Error fetching announcements: $e"
        })

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


            // 显示技能列表
            Text("Skills", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            skills.forEach { skill ->
                SkillItem(skill)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 添加学生成绩按钮
            Button(onClick = {
                navController.navigate("student_score_screen/$componentId")
            }) {
                Text("Add Student Scores")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 查看考试成绩按钮
            Button(onClick = {
                navController.navigate("exam_list/$componentId")
            }) {
                Text("View Exam Scores")
            }
        }
    }
}

@Composable
fun AnnouncementItem(announcement: Announcement) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(announcement.title, style = MaterialTheme.typography.body1)
            Text(announcement.content, style = MaterialTheme.typography.body2)
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

fun loadAnnouncements(
    db: FirebaseFirestore,
    componentId: String,
    limit: Long = 5,
    onSuccess: (List<Announcement>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    db.collection("components").document(componentId).collection("announcements").orderBy("timestamp", Query.Direction.DESCENDING).limit(limit).get()
        .addOnSuccessListener { result ->
            val announcements = result.documents.mapNotNull { document ->
                document.toObject(Announcement::class.java)?.copy(id = document.id)
            }
            onSuccess(announcements)
        }
        .addOnFailureListener { e ->
            onFailure(e)
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