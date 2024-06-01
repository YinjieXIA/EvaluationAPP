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
    var userComponents by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    userComponents = document.get("components") as? List<String> ?: emptyList()
                    db.collection("announcements").get()
                        .addOnSuccessListener { result ->
                            announcements = result.documents.mapNotNull { document ->
                                document.toObject(Announcement::class.java)?.copy(id = document.id)
                            }.filter { announcement ->
                                announcement.componentId == null || announcement.componentId in userComponents
                            }
                        }
                        .addOnFailureListener { e ->
                            // 处理错误
                        }
                }
                .addOnFailureListener { e ->
                    // 处理错误
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
    )
}
