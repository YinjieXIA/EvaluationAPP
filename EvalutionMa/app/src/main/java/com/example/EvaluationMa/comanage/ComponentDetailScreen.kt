package com.example.EvaluationMa.comanage

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import androidx.compose.ui.graphics.Color

data class Announcement(val id: String = "", val title: String = "", val content: String = "", val timestamp: Long = 0L)

@Composable
fun ComponentDetailScreen(navController: NavController, componentId: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var role by remember { mutableStateOf<String?>(null) }
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // 获取当前用户角色
        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    role = document.getString("role")
                }
                .addOnFailureListener { exception ->
                    Log.e("ComponentDetailScreen", "Error getting user role: $exception")
                }
        }

        // 获取公告
        db.collection("components").document(componentId).collection("announcements").orderBy("timestamp").limit(5).get()
            .addOnSuccessListener { result ->
                announcements = result.documents.mapNotNull { document ->
                    document.toObject(Announcement::class.java)?.copy(id = document.id)
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching announcements: $e"
                Log.e("ComponentDetailScreen", errorMessage)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Component Details", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        // 公告模块
        Text("Announcements", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            items(announcements) { announcement ->
                AnnouncementItem(announcement)
            }
        }
        Button(onClick = {
            navController.navigate("component_announcements/$componentId")
        }) {
            Text("More")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 发布公告模块 (仅限 component 管理员或总管理员)
        if (role == "component_manager" || role == "module_manager") {
            Text("Post Announcement", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val newAnnouncement = Announcement(title = title, content = content, timestamp = System.currentTimeMillis())
                db.collection("components").document(componentId).collection("announcements").add(newAnnouncement)
                    .addOnSuccessListener { documentReference ->
                        newAnnouncement.copy(id = documentReference.id)
                        announcements = listOf(newAnnouncement) + announcements
                        title = ""
                        content = ""
                    }
                    .addOnFailureListener { e ->
                        errorMessage = "Error posting announcement: $e"
                        Log.e("ComponentDetailScreen", errorMessage)
                    }
            }) {
                Text("Post")
            }
            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 课程成绩部分 (预留)
        Text("Course Grades", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        Text("This section is under construction.")
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