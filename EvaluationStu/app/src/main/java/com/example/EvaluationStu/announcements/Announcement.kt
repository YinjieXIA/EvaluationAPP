package com.example.EvaluationStu.announcements

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.EvaluationStu.models.Announcement
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AnnouncementsScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var studentGroup by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    studentGroup = document.getString("group")
                    studentGroup?.let { groupId ->
                        db.collection("announcements")
                            .whereEqualTo("groupId", groupId) // 查询指定群组的公告
                            .get()
                            .addOnSuccessListener { result ->
                                val targetedAnnouncements = result.documents.mapNotNull {
                                    it.toObject(Announcement::class.java)?.copy(id = it.id)
                                }
                                db.collection("announcements")
                                    .whereEqualTo("groupId", null) // 查询广播公告
                                    .get()
                                    .addOnSuccessListener { generalResult ->
                                        val generalAnnouncements = generalResult.documents.mapNotNull {
                                            it.toObject(Announcement::class.java)?.copy(id = it.id)
                                        }
                                        announcements = targetedAnnouncements + generalAnnouncements // 合并两类公告
                                    }
                            }
                            .addOnFailureListener { e ->
                                errorMessage = "Error fetching targeted announcements: ${e.message}"
                            }
                    }
                }
                .addOnFailureListener { e ->
                    errorMessage = "Error fetching user data: ${e.message}"
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Announcements") }
            )
        },
        content = {
            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "Unknown error",
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(announcements) { announcement ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { navController.navigate("announcement_detail/${announcement.id}") },
                            elevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = announcement.title,
                                        style = MaterialTheme.typography.h6
                                    )
                                    Text(
                                        text = "Published on: ${announcement.timestamp}",
                                        style = MaterialTheme.typography.body2
                                    )
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = "Details")
                            }
                        }
                    }
                }
            }
        }
    )
}