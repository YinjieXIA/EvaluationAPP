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
fun AssignTutorScreen(navController: NavController, groupId: String) {
    val db = FirebaseFirestore.getInstance()
    var components by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var tutors by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedComponent by remember { mutableStateOf<Map<String, Any>?>(null) }
    var selectedTutorId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.collection("components").get()
            .addOnSuccessListener { result ->
                components = result.documents.mapNotNull { document ->
                    val data = document.data
                    data?.put("uid", document.id)
                    data
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching components: $e"
            }

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

    fun assignTutor() {
        selectedComponent?.let { component ->
            val componentId = component["uid"] as String
            db.collection("groups").document(groupId).update("componentTutors.$componentId", selectedTutorId)
                .addOnSuccessListener {
                    errorMessage = "Tutor assigned successfully"
                    navController.popBackStack()
                }
                .addOnFailureListener { e ->
                    errorMessage = "Error assigning tutor: $e"
                }
        }
    }

    fun removeTutor() {
        selectedComponent?.let { component ->
            val componentId = component["uid"] as String
            db.collection("groups").document(groupId).update("componentTutors.$componentId", null)
                .addOnSuccessListener {
                    errorMessage = "Tutor removed successfully"
                    selectedTutorId = null
                }
                .addOnFailureListener { e ->
                    errorMessage = "Error removing tutor: $e"
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Assign Tutors to Component", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (components.isNotEmpty()) {
            LazyColumn {
                items(components) { component ->
                    ComponentItem(
                        component = component,
                        isSelected = selectedComponent == component,
                        onSelect = {
                            selectedComponent = component
                            selectedTutorId = component["assignedTutor"] as? String
                        }
                    )
                }
            }
        } else {
            Text("No components available.")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedComponent != null) {
            Text("Select Tutor for ${selectedComponent!!["name"]}", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))

            Box {
                Button(onClick = { expanded = true }) {
                    Text(selectedTutorId?.let { tutorId ->
                        tutors.find { it["uid"] == tutorId }?.let { "${it["firstName"]} ${it["lastName"]}" }
                    } ?: "Select Tutor")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    tutors.forEach { tutor ->
                        DropdownMenuItem(onClick = {
                            selectedTutorId = tutor["uid"] as? String
                            expanded = false
                        }) {
                            Text("${tutor["firstName"]} ${tutor["lastName"]}")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(onClick = { assignTutor() }) {
                    Text("Assign Tutor")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { removeTutor() }) {
                    Text("Remove Tutor")
                }
            }
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun ComponentItem(
    component: Map<String, Any>,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val componentName = component["name"] as? String ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(componentName)
        Button(
            onClick = onSelect,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isSelected) Color.Green else Color.White
            )
        ) {
            Text(if (isSelected) "Selected" else "Select")
        }
    }
}
