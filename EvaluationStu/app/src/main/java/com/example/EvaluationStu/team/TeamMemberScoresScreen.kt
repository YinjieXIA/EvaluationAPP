package com.example.EvaluationStu.team

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.EvaluationStu.models.ComponentScore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun TeamMemberScoresScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var teamMembers by remember { mutableStateOf<List<TeamMemberScore>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // 数据加载
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            currentUser?.let { user ->
                // 获取用户的 groupId 和 teamId
                val userDoc = db.collection("users").document(user.uid).get().await()
                val groupId = userDoc.getString("group") ?: return@launch
                val teamId = userDoc.getString("team") ?: return@launch

                // 获取团队成员成绩
                val teamMembersDocs = db.collection("groups").document(groupId)
                    .collection("teams").document(teamId).collection("students").get().await()

                val members = teamMembersDocs.documents.mapNotNull { document ->
                    val memberId = document.id

                    // 获取团队成员的用户文档，获取电子邮件和名称
                    val memberUserDoc = db.collection("users").document(memberId).get().await()
                    val email = memberUserDoc.getString("email") ?: ""
                    val firstName = memberUserDoc.getString("firstName") ?: ""
                    val lastName = memberUserDoc.getString("lastName") ?: ""
                    val name = "$firstName $lastName"

                    // 获取每个成员的所有组件的分数
                    val totalScoresDocuments = db.collection("groups").document(groupId)
                        .collection("teams").document(teamId).collection("students")
                        .document(memberId).collection("totalScores").get().await()

                    val componentScores = totalScoresDocuments.documents.mapNotNull { scoreDoc ->
                        val componentId = scoreDoc.getString("componentId") ?: return@mapNotNull null
                        val score = scoreDoc.getDouble("totalScore") ?: return@mapNotNull null
                        val componentDoc = db.collection("components").document(componentId).get().await()
                        val componentName = componentDoc.getString("name") ?: "Unknown Component"
                        ComponentScore(componentName, score, componentId)
                    }

                    TeamMemberScore(memberId, name, email, componentScores)
                }

                teamMembers = members
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team Member Scores") }
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Team Member Scores",
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(teamMembers) { member ->
                        TeamMemberCard(navController, member)
                    }
                }
            }
        }
    )
}

@Composable
fun TeamMemberCard(navController: NavController, member: TeamMemberScore) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { navController.navigate("team_member_detail/${member.id}") },
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.h6
            )
            Text(
                text = "Email: ${member.email}",
                style = MaterialTheme.typography.body2,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${member.email}")
                    }
                    context.startActivity(intent)
                }
            )
            member.scores.forEach { componentScore ->
                Text(
                    text = "Component: ${componentScore.name}, Score: ${componentScore.score}",
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }
}

data class TeamMemberScore(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val scores: List<ComponentScore> = emptyList()
)
