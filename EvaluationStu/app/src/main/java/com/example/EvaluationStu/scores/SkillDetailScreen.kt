package com.example.EvaluationStu.scores

import android.annotation.SuppressLint
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

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun SkillDetailScreen(navController: NavController, componentName: String, skillName: String) {
    var skillDetail by remember { mutableStateOf<SkillDetail?>(null) }

    // 模拟数据加载
    LaunchedEffect(componentName, skillName) {
        // 获取技能详情
        // TODO: 这里需要根据实际的Firestore数据库结构进行查询
        skillDetail = SkillDetail(
            name = skillName,
            description = "Description for $skillName",
            score = 16.0,
            comment = "Well done",
            scoreHistory = listOf(16, 15, 14)
        ) // 示例数据
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$componentName - $skillName Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = {
            skillDetail?.let { skill ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
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
            } ?: run {
                Text("Loading...", modifier = Modifier.padding(16.dp))
            }
        }
    )
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ScoreHistoryScreen(navController: NavController, skillName: String) {
    var scoreHistory by remember { mutableStateOf<List<Int>>(emptyList()) }

    // 模拟数据加载
    LaunchedEffect(skillName) {
        // 获取历史分数
        // TODO: 这里需要根据实际的Firestore数据库结构进行查询
        scoreHistory = listOf(16, 15, 14, 13, 12) // 示例数据
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                items(scoreHistory) { score ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = 4.dp
                    ) {
                        Text(
                            text = "Score: $score",
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    )
}