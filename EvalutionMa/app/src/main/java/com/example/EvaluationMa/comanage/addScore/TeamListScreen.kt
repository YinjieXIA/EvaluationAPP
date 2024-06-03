package com.example.EvaluationMa.comanage.addScore

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun TeamListScreen(navController: NavController, groupId: String, componentId: String) {
    val db = FirebaseFirestore.getInstance()
    var teams by remember { mutableStateOf<List<Team>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(groupId) {
        loadTeamsInGroup(groupId, onSuccess = { result ->
            teams = result
        }, onFailure = { e ->
            errorMessage = "Error fetching teams: $e"
            Log.e("TeamListScreen", errorMessage)
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Teams in Group", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (teams.isNotEmpty()) {
            LazyColumn {
                items(teams) { team ->
                    TeamItem(
                        team = team,
                        onTeamSelected = {
                            navController.navigate("student_score_list/${groupId}/${team.id}/$componentId")
                        }
                    )
                }
            }
        } else {
            Text("No teams found in this group.")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun TeamItem(team: Team, onTeamSelected: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onTeamSelected() },
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(team.name, style = MaterialTheme.typography.body1)
        }
    }
}

fun loadTeamsInGroup(
    groupId: String,
    onSuccess: (List<Team>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    db.collection("groups").document(groupId).collection("teams")
        .get()
        .addOnSuccessListener { result ->
            val teams = result.documents.mapNotNull { document ->
                document.toObject(Team::class.java)?.copy(id = document.id)
            }
            onSuccess(teams)
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}