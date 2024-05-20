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
fun AssignTutorScreen(navController: NavController, groupId: String, teamId: String) {
    val db = FirebaseFirestore.getInstance()
    var tutors by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedTutorIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("users").whereEqualTo("role", "tutor").get()
            .addOnSuccessListener { result ->
                tutors = result.documents.mapNotNull { document ->
                    val data = document.data
                    data?.put("uid", document.id)
                    data
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching tutors: $e"
            }
    }

    fun assignTutors(teamId: String, tutorIds: Set<String>) {
        db.collection("groups").document(groupId).collection("teams").document(teamId).update("tutors", tutorIds.toList())
            .addOnSuccessListener {
                errorMessage = "Tutors assigned successfully"
                navController.popBackStack()
            }
            .addOnFailureListener { e ->
                errorMessage = "Error assigning tutors: $e"
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Assign Tutors to Team", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (tutors.isNotEmpty()) {
            LazyColumn {
                items(tutors) { tutor ->
                    TutorSelectItem(
                        tutor = tutor,
                        isSelected = selectedTutorIds.contains(tutor["uid"] as String),
                        onSelect = { tutorId ->
                            selectedTutorIds = if (selectedTutorIds.contains(tutorId)) {
                                selectedTutorIds - tutorId
                            } else {
                                selectedTutorIds + tutorId
                            }
                        }
                    )
                }
            }
            Button(onClick = {
                assignTutors(teamId, selectedTutorIds)
            }) {
                Text("Assign Tutors")
            }
        } else {
            Text("No tutors available.")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun TutorSelectItem(
    tutor: Map<String, Any>,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    val tutorId = tutor["uid"] as? String ?: ""
    val tutorName = "${tutor["firstName"]} ${tutor["lastName"]}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(tutorName)
        Button(
            onClick = { onSelect(tutorId) },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isSelected) Color.Green else Color.White
            )
        ) {
            Text(if (isSelected) "Selected" else "Select")
        }
    }
}
