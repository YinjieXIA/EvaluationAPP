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
fun ScoreHistoryScreen(navController: NavController, skillName: String) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var scoreHistory by remember { mutableStateOf<List<SkillDetail>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 数据加载
    LaunchedEffect(skillName) {
        currentUser?.let { user ->
            try {
                val userDoc = db.collection("users").document(user.uid).get().await()
                val groupId = userDoc.getString("group") ?: return@LaunchedEffect
                val teamId = userDoc.getString("team") ?: return@LaunchedEffect

                Log.d("ScoreHistoryScreen", "Group ID: $groupId, Team ID: $teamId")

                // 获取技能评分历史
                val scoreDocs = db.collection("groups").document(groupId).collection("teams")
                    .document(teamId).collection("students").document(user.uid)
                    .collection("studentScores")
                    .whereEqualTo("skillId", skillName)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()

                Log.d("ScoreHistoryScreen", "Fetched ${scoreDocs.documents.size} documents")

                val skillHistoryList = scoreDocs.documents.mapNotNull { document ->
                    Log.d("ScoreHistoryScreen", "Document data: ${document.data}")
                    val score = document.getDouble("score") ?: 0.0
                    val timestamp = document.getLong("timestamp") ?: 0L

                    SkillDetail(
                        name = skillName,
                        description = "Score recorded at $timestamp",
                        score = score,
                        comment = "Score: $score",
                        scoreHistory = emptyList()
                    )
                }

                // 排除最新的成绩，只显示历史成绩
                scoreHistory = if (skillHistoryList.isNotEmpty()) skillHistoryList.drop(1) else emptyList()
                isLoading = false
            } catch (e: Exception) {
                Log.e("ScoreHistoryScreen", "Error fetching score history", e)
                isLoading = false
            }
        } ?: run {
            isLoading = false
            Log.e("ScoreHistoryScreen", "Current user is null")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$skillName Score History") },
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
                if (scoreHistory.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No score history found.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(scoreHistory) { skill ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                elevation = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = skill.description,
                                        style = MaterialTheme.typography.body1
                                    )
                                    Text(
                                        text = "Score: ${skill.score}",
                                        style = MaterialTheme.typography.body1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
