package com.example.EvaluationMa.tutormanage

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.EvaluationMa.admin.Announcement
import com.example.EvaluationMa.admin.postAnnouncement
import com.example.EvaluationMa.admin.AnnouncementItem
import com.example.EvaluationMa.comanage.Component
import com.example.EvaluationMa.comanage.PostAnnouncementDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.compose.foundation.lazy.items

@Composable
fun TutorAnnouncementScreen(navController: NavController, componentId: String, groupId: String) {
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

    LaunchedEffect(key1 = Pair(componentId, groupId)) {
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
        loadGroupAnnouncements(db, componentId, groupId, onSuccess = { result ->
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
        Text("Group Announcements for ${selectedComponent?.name}", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (role == "tutor") {
            Button(onClick = { showPostAnnouncement = true }) {
                Text("Post Announcement to Group")
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
                        groupId
                    ){
                        showPostAnnouncement = false
                        // Reload announcements
                        loadGroupAnnouncements(db, componentId, groupId, onSuccess = {
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

fun loadGroupAnnouncements(
    db: FirebaseFirestore,
    componentId: String,
    groupId: String,
    onSuccess: (List<Announcement>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    db.collection("announcements")
        .whereEqualTo("componentId", componentId)
        .get()
        .addOnSuccessListener { result ->
            val announcements = result.documents.mapNotNull { document ->
                document.toObject(Announcement::class.java)
            }.filter { it.groupId == groupId }
                .sortedByDescending { it.timestamp }
            onSuccess(announcements)
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}