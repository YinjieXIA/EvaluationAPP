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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore


data class Group(val id: String = "", val name: String = "")
data class Team(val id: String = "", val name: String = "")
data class StudentScore(
    val score: Double,
    val timestamp: Timestamp,
    val skillId: String,
    val componentId: String
)

@Composable
fun StudentScoreScreen(navController: NavController, componentId: String) {
    val db = FirebaseFirestore.getInstance()
    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(componentId) {
        loadGroupsContainingComponent(componentId, onSuccess = { result ->
            groups = result
        }, onFailure = { e ->
            errorMessage = "Error fetching groups: $e"
            Log.e("StudentScoreScreen", errorMessage)
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Student Scores", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (groups.isNotEmpty()) {
            LazyColumn {
                items(groups) { group ->
                    GroupItem(
                        group = group,
                        onGroupSelected = {
                            navController.navigate("team_list/${group.id}/$componentId")
                        }
                    )
                }
            }
        } else {
            Text("No groups found containing this component.")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun GroupItem(group: Group, onGroupSelected: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onGroupSelected() },
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(group.name, style = MaterialTheme.typography.body1)
        }
    }
}

fun loadGroupsContainingComponent(
    componentId: String,
    onSuccess: (List<Group>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    db.collection("groups")
        .whereArrayContains("components", componentId)
        .get()
        .addOnSuccessListener { result ->
            val groups = result.documents.mapNotNull { document ->
                document.toObject(Group::class.java)?.copy(id = document.id)
            }
            onSuccess(groups)
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}
