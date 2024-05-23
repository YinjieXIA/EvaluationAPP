package com.example.EvaluationMa.admin.studentManage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AssignComponentScreen(navController: NavController, groupId: String) {
    val db = FirebaseFirestore.getInstance()
    var components by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var tutors by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedComponentId by remember { mutableStateOf<String?>(null) }
    var selectedTutorId by remember { mutableStateOf<String?>(null) }
    var selectedComponents by remember { mutableStateOf<Set<String>>(emptySet()) }
    var errorMessage by remember { mutableStateOf("") }

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

        db.collection("groups").document(groupId).get()
            .addOnSuccessListener { document ->
                val data = document.data
                val existingComponents = data?.get("components") as? List<String> ?: emptyList()
                selectedComponents = existingComponents.toSet()
                val componentTutors = data?.get("componentTutors") as? Map<String, String> ?: emptyMap()
                components = components.map { component ->
                    val componentId = component["uid"] as? String ?: ""
                    component.toMutableMap().apply {
                        put("selected", existingComponents.contains(componentId))
                        put("tutor", componentTutors[componentId] ?: "None")
                    }
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching group data: $e"
            }
    }

    fun assignTutor(componentId: String, tutorId: String?) {
        if (!selectedComponents.contains(componentId)) {
            errorMessage = "Component must be selected before assigning a tutor"
            return
        }
        db.collection("groups").document(groupId).update("componentTutors.$componentId", tutorId)
            .addOnSuccessListener {
                components = components.map { component ->
                    if (component["uid"] == componentId) {
                        component.toMutableMap().apply { put("tutor", tutorId ?: "None") }
                    } else component
                }
                selectedTutorId = tutorId
                errorMessage = "Tutor assigned successfully"
            }
            .addOnFailureListener { e ->
                errorMessage = "Error assigning tutor: $e"
            }
    }

    fun removeTutor(componentId: String) {
        db.collection("groups").document(groupId).update("componentTutors.$componentId", FieldValue.delete())
            .addOnSuccessListener {
                components = components.map { component ->
                    if (component["uid"] == componentId) {
                        component.toMutableMap().apply { put("tutor", "None") }
                    } else component
                }
                selectedTutorId = null
                errorMessage = "Tutor removed successfully"
            }
            .addOnFailureListener { e ->
                errorMessage = "Error removing tutor: $e"
            }
    }

    fun toggleComponentSelection(componentId: String) {
        val isSelected = selectedComponents.contains(componentId)
        val newSelectedComponents = if (isSelected) {
            selectedComponents - componentId
        } else {
            selectedComponents + componentId
        }

        db.collection("groups").document(groupId).update("components", if (isSelected) FieldValue.arrayRemove(componentId) else FieldValue.arrayUnion(componentId))
            .addOnSuccessListener {
                selectedComponents = newSelectedComponents
                components = components.map { component ->
                    if (component["uid"] == componentId) {
                        component.toMutableMap().apply {
                            put("selected", !isSelected)
                            if (isSelected) {
                                put("tutor", "None")
                                db.collection("groups").document(groupId).update("componentTutors.$componentId", FieldValue.delete())
                            }
                        }
                    } else component
                }
                errorMessage = if (isSelected) "Component unselected successfully" else "Component selected successfully"
            }
            .addOnFailureListener { e ->
                errorMessage = if (isSelected) "Error unselecting component: $e" else "Error selecting component: $e"
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Assign Tutors to Components", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (components.isNotEmpty()) {
            LazyColumn {
                items(components) { component ->
                    val componentId = component["uid"] as String
                    val tutorName = component["tutor"]?.let { tutorId ->
                        tutors.find { it["uid"] == tutorId }?.let { "${it["firstName"]} ${it["lastName"]}" } ?: "None"
                    } ?: "None"
                    ComponentItem(
                        component = component,
                        tutorName = tutorName,
                        isSelected = component["selected"] as? Boolean ?: false,
                        onSelect = {
                            toggleComponentSelection(componentId)
                        },
                        onEdit = {
                            selectedComponentId = componentId
                            selectedTutorId = component["tutor"] as? String
                        },
                        onDelete = {
                            removeTutor(componentId)
                        }
                    )
                    if (selectedComponentId == componentId && component["selected"] == true) {
                        TutorSelection(
                            tutors = tutors,
                            selectedTutorId = selectedTutorId,
                            onTutorSelected = { tutorId ->
                                assignTutor(componentId, tutorId)
                                selectedComponentId = null // 自动刷新后让选择界面消失
                            }
                        )
                    }
                }
            }
        } else {
            Text("No components available.")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun ComponentItem(
    component: Map<String, Any>,
    tutorName: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
        Row {
            Text(tutorName)
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Tutor")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Tutor")
            }
        }
        Button(
            onClick = onSelect,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isSelected) Color.Blue else Color.White,
                contentColor = if (isSelected) Color.White else Color.Blue
            )
        ) {
            Text(if (isSelected) "Selected" else "Select")
        }
    }
}

@Composable
fun TutorSelection(
    tutors: List<Map<String, Any>>,
    selectedTutorId: String?,
    onTutorSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
                    onTutorSelected(tutor["uid"] as String)
                    expanded = false
                }) {
                    Text("${tutor["firstName"]} ${tutor["lastName"]}")
                }
            }
        }
    }
}
