package com.example.EvaluationStu.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun StudentHomeScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var studentName by remember { mutableStateOf("") }
    var performanceSummary by remember { mutableStateOf("") }
    var announcements by remember { mutableStateOf<List<String>>(emptyList()) }
    var components by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    // 模拟数据加载
    LaunchedEffect(Unit) {
        // 获取学生姓名
        currentUser?.let {
            db.collection("users").document(it.uid).get()
                .addOnSuccessListener { document ->
                    studentName = document.getString("firstName") ?: "Student" // TODO: 将此示例数据替换为实际数据
                }
        }

        // 获取学期成绩概览
        // TODO: 这里需要根据实际的Firestore数据库结构进行查询
        performanceSummary = "Semester average score: 15.2 / 20" // 示例数据

        // 获取公告摘要
        db.collection("announcements").limit(3).get()
            .addOnSuccessListener { result ->
                announcements = result.documents.map { it.getString("title") ?: "Unknown Announcement" } // TODO: 将此示例数据替换为实际数据
            }

        // 获取学生被分配的components及其权重
        // TODO: 根据实际数据结构进行调整
        components = listOf(
            "Générales E-S" to "20%",
            "Electronique" to "50%",
            "Signal" to "30%",
            "Générales I-T" to "20%",
            "Informatique" to "50%",
            "Télécommunications" to "30%",
            "Intégration" to "100%"
        ) // 示例数据
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Welcome back, $studentName",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Current Semester Performance", style = MaterialTheme.typography.h6)
                    Text(text = performanceSummary, style = MaterialTheme.typography.body1)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Components and Weights", style = MaterialTheme.typography.h6)
                    components.forEach { (component, weight) ->
                        Text(text = "$component: $weight", style = MaterialTheme.typography.body1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Scoring Standards", style = MaterialTheme.typography.h6)
                    // TODO: 根据实际数据结构进行调整
                    val standards = listOf(
                        "Not Acquired" to "0",
                        "Far" to "7",
                        "Close" to "10",
                        "Very Close" to "13",
                        "Expected" to "16",
                        "Beyond" to "20",
                        "Missing Evaluation" to "-",
                        "Not Applicable" to "-"
                    )
                    standards.forEach { (level, score) ->
                        Text(text = "$level: $score", style = MaterialTheme.typography.body1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Announcements Summary", style = MaterialTheme.typography.h6)
                    announcements.forEach { announcement ->
                        Text(text = announcement, style = MaterialTheme.typography.body1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Button(
                onClick = { navController.navigate("grades") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Icon(Icons.Default.Grade, contentDescription = "View Grades", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Grades")
            }

            Button(
                onClick = { navController.navigate("request_review") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Icon(Icons.Default.Assessment, contentDescription = "Request Re-evaluation", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Request Re-evaluation")
            }

            Button(
                onClick = { navController.navigate("announcements") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Icon(Icons.Default.Announcement, contentDescription = "View Announcements", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Announcements")
            }
        }
    }
}
