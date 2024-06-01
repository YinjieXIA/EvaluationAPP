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
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun TeamMemberScoresScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var teamMembers by remember { mutableStateOf<List<TeamMemberScore>>(emptyList()) }

    // 模拟数据加载
    LaunchedEffect(Unit) {
        // 获取团队成员成绩
        db.collection("team_members").get()
            .addOnSuccessListener { result ->
                teamMembers = result.documents.map { it.toObject(TeamMemberScore::class.java) ?: TeamMemberScore() }
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

                // 排序和筛选控件
                // TODO: 添加排序和筛选功能

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
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
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
                Text(
                    text = "Score: ${member.score}",
                    style = MaterialTheme.typography.body1
                )
            }
            Icon(Icons.Default.ArrowForward, contentDescription = "Details")
        }
    }
}

data class TeamMemberScore(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val score: Double = 0.0
)
