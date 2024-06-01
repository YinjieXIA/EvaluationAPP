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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun TeamMemberDetailScreen(navController: NavController, memberId: String) {
    val db = FirebaseFirestore.getInstance()
    var memberDetail by remember { mutableStateOf<TeamMemberDetail?>(null) }

    // 模拟数据加载
    LaunchedEffect(memberId) {
        // 获取团队成员详细信息
        db.collection("team_members").document(memberId).get()
            .addOnSuccessListener { document ->
                memberDetail = document.toObject(TeamMemberDetail::class.java)
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
                        text = "Overall Score: ${detail.overallScore}",
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
                                    Text(
                                        text = "Comment: ${skill.comment}",
                                        style = MaterialTheme.typography.body2
                                    )
                                }
                            }
                        }
                    }
                }
            } ?: run {
                Text("Loading...", modifier = Modifier.padding(16.dp))
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
    val score: Double = 0.0,
    val comment: String = ""
)
