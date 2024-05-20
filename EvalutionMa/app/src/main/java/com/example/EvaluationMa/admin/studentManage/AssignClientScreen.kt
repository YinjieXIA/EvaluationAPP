package com.example.EvaluationMa.admin.studentManage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AssignClientScreen(navController: NavController, groupId: String) {
    val db = FirebaseFirestore.getInstance()
    var clients by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedClientId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("users").whereEqualTo("role", "client").get()
            .addOnSuccessListener { result ->
                clients = result.documents.mapNotNull { document ->
                    val data = document.data
                    data?.put("uid", document.id)
                    data
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching clients: $e"
            }
    }

    fun assignClient(groupId: String, clientId: String) {
        db.collection("groups").document(groupId).update("client", clientId)
            .addOnSuccessListener {
                errorMessage = "Client assigned successfully"
                navController.popBackStack()
            }
            .addOnFailureListener { e ->
                errorMessage = "Error assigning client: $e"
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Assign Client to Group", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (clients.isNotEmpty()) {
            LazyColumn {
                items(clients) { client ->
                    ClientSelectItem(
                        client = client,
                        isSelected = selectedClientId == client["uid"],
                        onSelect = { clientId ->
                            selectedClientId = clientId
                        }
                    )
                }
            }
            Button(onClick = {
                selectedClientId?.let { clientId ->
                    assignClient(groupId, clientId)
                }
            }) {
                Text("Assign Client")
            }
        } else {
            Text("No clients available.")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun ClientSelectItem(
    client: Map<String, Any>,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    val clientId = client["uid"] as? String ?: ""
    val clientName = "${client["firstName"]} ${client["lastName"]}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(clientName)
        Button(
            onClick = { onSelect(clientId) },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isSelected) Color.Green else Color.White
            )
        ) {
            Text(if (isSelected) "Selected" else "Select")
        }
    }
}
