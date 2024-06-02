package com.example.EvaluationStu.team

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun TeamMemberDetailScreen(navController: NavController, memberId: String) {
    val db = FirebaseFirestore.getInstance()
    var memberDetail by remember { mutableStateOf<TeamMemberDetail?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // 数据加载
    LaunchedEffect(memberId) {
        coroutineScope.launch {
            try {
                // 获取团队成员详细信息
                val memberDoc = db.collection("users").document(memberId).get().await()
                val groupId = memberDoc.getString("group") ?: return@launch
                val teamId = memberDoc.getString("team") ?: return@launch
                val name = memberDoc.getString("firstName") + " " + (memberDoc.getString("lastName") ?: "")
                val email = memberDoc.getString("email") ?: "Unknown"

                // 获取团队成员的技能成绩
                val scoreDocs = db.collection("groups").document(groupId).collection("teams")
                    .document(teamId).collection("students").document(memberId)
                    .collection("studentScores").get().await()

                val skillDetailList = scoreDocs.documents.groupBy { it.getString("skillId") }
                    .mapValues { it.value.maxByOrNull { doc -> doc.getLong("timestamp") ?: 0L } }
                    .mapNotNull { (_, document) ->
                        Log.d("TeamMemberDetailScreen", "Document data: ${document?.data}")
                        val skillId = document?.getString("skillId") ?: return@mapNotNull null
                        val score = document.getDouble("score") ?: 0.0

                        // 获取技能的详细信息，包括标题
                        val componentId = document.getString("componentId") ?: return@mapNotNull null
                        val skillDoc = db.collection("components").document(componentId)
                            .collection("skills").document(skillId).get().await()
                        val skillName = skillDoc.getString("title") ?: skillId

                        SkillDetail(
                            name = skillName,
                            score = score
                        )
                    }

                memberDetail = TeamMemberDetail(
                    name = name,
                    email = email,
                    overallScore = skillDetailList.sumByDouble { it.score } / skillDetailList.size,
                    skills = skillDetailList
                )
            } catch (e: Exception) {
                Log.e("TeamMemberDetailScreen", "Error fetching data", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team Member Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = {
            memberDetail?.let { detail ->
                val context = LocalContext.current
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = detail.name,
                        style = MaterialTheme.typography.h5,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .clickable {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:${detail.email}")
                                }
                                context.startActivity(intent)
                            }
                    )
                    Text(
                        text = "Email: ${detail.email}",
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Overall Score: %.2f".format(detail.overallScore),
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(detail.skills) { skill ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                elevation = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = skill.name,
                                        style = MaterialTheme.typography.h6
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
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    )
}

data class TeamMemberDetail(
    val name: String = "",
    val email: String = "",
    val overallScore: Double = 0.0,
    val skills: List<SkillDetail> = emptyList()
)

data class SkillDetail(
    val name: String = "",
    val score: Double = 0.0
)
