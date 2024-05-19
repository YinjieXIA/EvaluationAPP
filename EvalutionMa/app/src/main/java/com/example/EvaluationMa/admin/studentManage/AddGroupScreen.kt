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
fun AddGroupScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var groupName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add Group", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Group Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = {
            val newGroup = mapOf("name" to groupName)
            db.collection("groups").add(newGroup)
                .addOnSuccessListener {
                    navController.popBackStack()
                }
                .addOnFailureListener { e ->
                    errorMessage = "Error adding group: $e"
                }
        }) {
            Text("Add")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
