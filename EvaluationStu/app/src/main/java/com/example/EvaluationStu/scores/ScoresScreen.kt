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
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.EvaluationStu.models.ComponentScore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ScoresScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var studentName by remember { mutableStateOf("") }
    var components by remember { mutableStateOf<List<ComponentScore>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // 数据加载
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            // 获取学生姓名
            currentUser?.let { user ->
                val userDocument = db.collection("users").document(user.uid).get().await()
                studentName = userDocument.getString("firstName") ?: "Student"

                // 获取组件总成绩
                val teamId = userDocument.getString("team") ?: ""
                if (teamId.isNotEmpty()) {
                    val groupDocument = db.collection("groups").document(userDocument.getString("group")!!).get().await()
                    val totalScoresDocuments = db.collection("groups").document(groupDocument.id).collection("teams").document(teamId).collection("students").document(user.uid).collection("totalScores").get().await()

                    val componentScores = mutableListOf<ComponentScore>()
                    for (document in totalScoresDocuments.documents) {
                        val componentId = document.getString("componentId") ?: continue
                        val score = document.getDouble("totalScore") ?: continue
                        val componentDoc = db.collection("components").document(componentId).get().await()
                        val componentName = componentDoc.getString("name") ?: "Unknown Component"
                        componentScores.add(ComponentScore(componentName, score, componentId))
                    }
                    components = componentScores
                }
            }
        }
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
                                .clickable { navController.navigate("score_detail/${component.id}") },
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
