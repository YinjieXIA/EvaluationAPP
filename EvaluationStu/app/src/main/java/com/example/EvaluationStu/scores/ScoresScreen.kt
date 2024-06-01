package com.example.EvaluationStu.scores

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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.EvaluationStu.models.ComponentScore

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ScoresScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var studentName by remember { mutableStateOf("") }
    var components by remember { mutableStateOf<List<ComponentScore>>(emptyList()) }

    // 模拟数据加载
    LaunchedEffect(Unit) {
        // 获取学生姓名
        currentUser?.let {
            db.collection("users").document(it.uid).get()
                .addOnSuccessListener { document ->
                    studentName = document.getString("firstName") ?: "Student" // TODO: 将此示例数据替换为实际数据
                }
        }

        // 获取组件成绩
        // TODO: 这里需要根据实际的Firestore数据库结构进行查询
        components = listOf(
            ComponentScore("Générales E-S", 11.71),
            ComponentScore("Electronique", 13.80),
            ComponentScore("Signal", 13.23),
            ComponentScore("Générales I-T", 12.50),
            ComponentScore("Informatique", 14.00),
            ComponentScore("Télécommunications", 13.50),
            ComponentScore("Intégration", 15.00)
        ) // 示例数据
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scores Overview") }
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Scores for $studentName",
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(components) { component ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { navController.navigate("score_detail/${component.name}") },
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
                                        text = component.name,
                                        style = MaterialTheme.typography.h6
                                    )
                                    Text(
                                        text = "Score: ${component.score}",
                                        style = MaterialTheme.typography.body1
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