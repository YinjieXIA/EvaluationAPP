package com.example.EvaluationStu.announcements

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.EvaluationStu.models.Announcement
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AnnouncementDetailScreen(navController: NavController, announcementId: String) {
    val db = FirebaseFirestore.getInstance()
    var announcement by remember { mutableStateOf<Announcement?>(null) }

    // 模拟数据加载
    LaunchedEffect(announcementId) {
        // 获取公告详情
        db.collection("announcements").document(announcementId).get()
            .addOnSuccessListener { document ->
                announcement = document.toObject(Announcement::class.java)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Announcement Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = {
            announcement?.let { ann ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    item {
                        Text(
                            text = ann.title,
                            style = MaterialTheme.typography.h5,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    item {
                        Text(
                            text = "Published on: ${ann.timestamp}", // 这里可以格式化日期
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    item {
                        Text(
                            text = "Sent by: ${ann.sender}",
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    item {
                        Text(
                            text = "Component: ${ann.component}",
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    item {
                        Text(
                            text = ann.content,
                            style = MaterialTheme.typography.body1
                        )
                    }
                }
            } ?: run {
                Text("Loading...", modifier = Modifier.padding(16.dp))
            }
        }
    )
}
