package com.example.EvaluationStu.scores

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.EvaluationStu.models.SkillDetail
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun SkillDetailScreen(navController: NavController, componentId: String) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var skillDetails by remember { mutableStateOf<List<SkillDetail>>(emptyList()) }
    var componentName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // 数据加载
    LaunchedEffect(componentId) {
        currentUser?.let { user ->
            Log.d("SkillDetailScreen", "Fetching skill details for componentId: $componentId")

            try {
                // 从 users 表中获取当前学生所在的组和小组的 ID
                val userDoc = db.collection("users").document(user.uid).get().await()
                val groupId = userDoc.getString("group") ?: return@LaunchedEffect
                val teamId = userDoc.getString("team") ?: return@LaunchedEffect

                Log.d("SkillDetailScreen", "Group ID: $groupId, Team ID: $teamId")

                // 获取技能评分详情
                val scoreDocs = db.collection("groups").document(groupId).collection("teams")
                    .document(teamId).collection("students").document(user.uid)
                    .collection("studentScores")
                    .whereEqualTo("componentId", componentId)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()

                Log.d("SkillDetailScreen", "Fetched ${scoreDocs.documents.size} documents")

                val skillDetailList = scoreDocs.documents.groupBy { it.getString("skillId") }
                    .mapValues { it.value.maxByOrNull { doc -> doc.getLong("timestamp") ?: 0L } }
                    .mapNotNull { (_, document) ->
                        Log.d("SkillDetailScreen", "Document data: ${document?.data}")
                        val skillId = document?.getString("skillId") ?: return@mapNotNull null
                        val score = document.getDouble("score") ?: 0.0

                        // 获取技能的详细信息，包括标题
                        val skillDoc = db.collection("components").document(componentId)
                            .collection("skills").document(skillId).get().await()
                        val skillName = skillDoc.getString("title") ?: skillId
                        val skillDescription = skillDoc.getString("description") ?: "Description for $skillId"

                        SkillDetail(
                            name = skillName,
                            description = skillDescription,
                            score = score,
                            comment = "Well done",
                            scoreHistory = scoreDocs.documents.filter { it.getString("skillId") == skillId }.mapNotNull { it.getDouble("score") } // 存储所有成绩用于历史记录
                        )
                    }

                skillDetails = skillDetailList
                isLoading = false

                // 获取 component name
                val componentDoc = db.collection("components").document(componentId).get().await()
                componentName = componentDoc.getString("name") ?: "Unknown Component"
                Log.d("SkillDetailScreen", "Component name: $componentName")

            } catch (e: Exception) {
                Log.e("SkillDetailScreen", "Error fetching data", e)
                isLoading = false
            }
        } ?: run {
            isLoading = false
            Log.e("SkillDetailScreen", "Current user is null")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$componentName Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (skillDetails.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No skill details found.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(skillDetails) { skill ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = skill.name,
                                    style = MaterialTheme.typography.h5,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Text(
                                    text = "Description: ${skill.description}",
                                    style = MaterialTheme.typography.body1
                                )
                                Text(
                                    text = "Current Score: ${skill.score}",
                                    style = MaterialTheme.typography.body1
                                )
                                Text(
                                    text = "Comment: ${skill.comment}",
                                    style = MaterialTheme.typography.body2
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { navController.navigate("score_history/${skill.name}") },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("View Score History")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { navController.navigate("request_review/${skill.name}") },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Request Re-evaluation")
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
