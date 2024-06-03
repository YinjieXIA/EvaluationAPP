package com.example.EvaluationMa.comanage

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.EvaluationMa.admin.postAnnouncement
import com.example.EvaluationMa.comanage.Component
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun ComponentAnnouncementScreen(navController: NavController, componentId: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var role by remember { mutableStateOf<String?>(null) }
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var selectedComponent by remember { mutableStateOf<Component?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var showPostAnnouncement by remember { mutableStateOf(false) }
    var senderName by remember { mutableStateOf("") }

    LaunchedEffect(key1 = componentId) {
        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    role = document.getString("role")
                    senderName = document.getString("firstName") + " " + document.getString("lastName")
                }
                .addOnFailureListener { exception ->
                    errorMessage = "Error getting user info: $exception"
                }
        }

        // 获取指定组件信息
        db.collection("components").document(componentId).get()
            .addOnSuccessListener { document ->
                selectedComponent = document.toObject(Component::class.java)?.copy(id = document.id)
            }
            .addOnFailureListener { exception ->
                errorMessage = "Error fetching component: $exception"
            }

        // 获取该组件的公告
        loadAnnouncementsForComponent(db, componentId, onSuccess = { result ->
            announcements = result
        }, onFailure = { e ->
            errorMessage = "Error fetching announcements: $e"
        })
    }

    if (errorMessage.isNotEmpty()) {
        Text(errorMessage, color = Color.Red, modifier = Modifier.padding(16.dp))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Component Announcements", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (role == "component_manager") {
            Button(onClick = { showPostAnnouncement = true }) {
                Text("Post Announcement")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(announcements) { announcement ->
                AnnouncementItem(announcement)
            }
        }

        if (showPostAnnouncement) {
            PostAnnouncementDialog(
                showPostAnnouncement,
                title,
                content,
                selectedComponent,
                senderName,
                onTitleChange = { title = it },
                onContentChange = { content = it },
                onDismiss = { showPostAnnouncement = false },
                onPost = {
                    postAnnouncement(
                        db,
                        selectedComponent?.id,
                        title,
                        content,
                        senderName,
                        null  // 没有群组ID，因为是发送给组件的公告
                    ){
                        showPostAnnouncement = false
                        // Reload announcements
                        loadAnnouncementsForComponent(db, componentId, onSuccess = {
                            announcements = it
                        }, onFailure = {
                            errorMessage = "Error fetching announcements: $it"
                        })
                    }
                    showPostAnnouncement = false
                }
            )
        }
    }
}

fun loadAnnouncementsForComponent(
    db: FirebaseFirestore,
    componentId: String,
    limit: Long = 5,
    onSuccess: (List<Announcement>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    db.collection("announcements")
        .whereEqualTo("componentId", componentId)
        .limit(limit)
        .get()
        .addOnSuccessListener { result ->
            val announcements = result.documents.mapNotNull { document ->
                document.toObject(Announcement::class.java)
            }.sortedByDescending { it.timestamp } // 在客户端按时间戳降序排序
            onSuccess(announcements)
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}

@Composable
fun PostAnnouncementDialog(
    show: Boolean,
    title: String,
    content: String,
    selectedComponent: Component?,
    senderName: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onPost: () -> Unit
) {
    if (show) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                shape = MaterialTheme.shapes.medium,
                elevation = 8.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("New Announcement", style = MaterialTheme.typography.h6)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 组件名称显示（不可编辑）
                    Text("Posting to: ${selectedComponent?.name ?: "No Component Selected"}")

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text("Title") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = content,
                        onValueChange = onContentChange,
                        label = { Text("Content") },
                        modifier = Modifier.height(140.dp),
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = onPost) {
                            Text("Post")
                        }
                        Button(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}