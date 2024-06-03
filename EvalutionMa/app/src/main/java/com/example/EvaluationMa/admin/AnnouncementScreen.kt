package com.example.EvaluationMa.admin

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.EvaluationMa.comanage.Component
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class Announcement(
    val title: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val sender: String = "",
    val componentId: String? = null, // 可选字段，为空时表示群发
    val groupId: String? = null // 可选字段
)

@Composable
fun AnnouncementScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var role by remember { mutableStateOf<String?>(null) }
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var components by remember { mutableStateOf<List<Component>>(emptyList()) }
    var selectedComponent by remember { mutableStateOf<Component?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var showPostAnnouncement by remember { mutableStateOf(false) }
    var senderName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // 获取当前用户信息
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

        // 获取组件
        loadComponents(db, onSuccess = { result ->
            components = result
        }, onFailure = { e ->
            errorMessage = "Error fetching components: $e"
        })

        // 获取公告
        loadAnnouncements(db, onSuccess = { result ->
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
        Text("Announcements", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        //if (role == "module_manager") {
            Button(onClick = { showPostAnnouncement = true }) {
                Text("Post Announcement")
            }
        //}

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(announcements) { announcement ->
                AnnouncementItem(announcement)
            }
        }

        if (showPostAnnouncement) {
            Dialog(onDismissRequest = { showPostAnnouncement = false }) {
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

                        // 组件选择下拉框
                        var expanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            TextField(
                                value = selectedComponent?.name ?: "Select Component (Optional)",
                                onValueChange = {},
                                label = { Text("Component") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = true },
                                enabled = false,
                                readOnly = true
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                components.forEach { component ->
                                    DropdownMenuItem(onClick = {
                                        selectedComponent = component
                                        expanded = false
                                    }) {
                                        Text(text = component.name)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text("Content") }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(onClick = {
                                if (title.isNotEmpty() && content.isNotEmpty()) {
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
                                        loadAnnouncements(db, onSuccess = {
                                            announcements = it
                                        }, onFailure = {
                                            errorMessage = "Error fetching announcements: $it"
                                        })
                                    }
                                    showPostAnnouncement = false
                                }
                            }) {
                                Text("Post")
                            }
                            Button(onClick = { showPostAnnouncement = false }) {
                                Text("Cancel")
                            }
                        }
                    }
                }
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

fun postAnnouncement(
    db: FirebaseFirestore,
    componentId: String?,
    title: String,
    content: String,
    sender: String,
    groupId: String?,
    onSuccess: () -> Unit
) {
    val timestamp = System.currentTimeMillis()
    val announcement = Announcement(
        title = title,
        content = content,
        timestamp = timestamp,
        sender = sender,
        componentId = componentId,
        groupId = groupId
    )

    db.collection("announcements").add(announcement)
        .addOnSuccessListener {
            Log.d("AnnouncementScreen", "Announcement posted successfully")
            onSuccess()  // Call onSuccess callback to reload data
        }
        .addOnFailureListener { e ->
            Log.e("AnnouncementScreen", "Error posting announcement", e)
        }
}

fun loadComponents(
    db: FirebaseFirestore,
    onSuccess: (List<Component>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    db.collection("components").get()
        .addOnSuccessListener { result ->
            val components = result.documents.mapNotNull { document ->
                document.toObject(Component::class.java)?.copy(id = document.id)
            }
            onSuccess(components)
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}

fun loadAnnouncements(
    db: FirebaseFirestore,
    limit: Long = 5,
    onSuccess: (List<Announcement>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    db.collection("announcements")
        .orderBy("timestamp", Query.Direction.DESCENDING).limit(limit).get()
        .addOnSuccessListener { result ->
            val announcements = result.documents.mapNotNull { document ->
                document.toObject(Announcement::class.java)
            }
            onSuccess(announcements)
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}