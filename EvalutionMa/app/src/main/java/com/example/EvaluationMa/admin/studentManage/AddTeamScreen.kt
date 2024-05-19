package com.example.EvaluationMa.admin.studentManage

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AddTeamScreen(navController: NavController, groupId: String) {
    val db = FirebaseFirestore.getInstance()
    var teamName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add Team", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = teamName,
            onValueChange = { teamName = it },
            label = { Text("Team Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = {
            val newTeam = mapOf("name" to teamName)
            db.collection("groups").document(groupId).collection("teams").add(newTeam)
                .addOnSuccessListener {
                    navController.popBackStack()
                }
                .addOnFailureListener { e ->
                    errorMessage = "Error adding team: $e"
                }
        }) {
            Text("Add")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
