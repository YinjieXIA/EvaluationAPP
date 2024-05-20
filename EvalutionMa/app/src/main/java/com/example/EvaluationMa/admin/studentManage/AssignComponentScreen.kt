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
fun AssignComponentScreen(navController: NavController, groupId: String) {
    val db = FirebaseFirestore.getInstance()
    var components by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedComponentIds by remember { mutableStateOf<Set<String>>(emptySet()) }
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
    }

    fun assignComponents(groupId: String, componentIds: Set<String>) {
        db.collection("groups").document(groupId).update("components", componentIds.toList())
            .addOnSuccessListener {
                errorMessage = "Components assigned successfully"
                navController.popBackStack()
            }
            .addOnFailureListener { e ->
                errorMessage = "Error assigning components: $e"
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Assign Components to Group", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (components.isNotEmpty()) {
            LazyColumn {
                items(components) { component ->
                    ComponentChip(
                        component = component,
                        isSelected = selectedComponentIds.contains(component["uid"] as String),
                        onSelect = { componentId ->
                            selectedComponentIds = if (selectedComponentIds.contains(componentId)) {
                                selectedComponentIds - componentId
                            } else {
                                selectedComponentIds + componentId
                            }
                        }
                    )
                }
            }
            Button(onClick = {
                assignComponents(groupId, selectedComponentIds)
            }) {
                Text("Assign Components")
            }
        } else {
            Text("No components available.")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ComponentChip(
    component: Map<String, Any>,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    val componentId = component["uid"] as? String ?: ""
    val componentName = component["name"] as? String ?: ""

    Chip(
        onClick = { onSelect(componentId) },
        colors = ChipDefaults.chipColors(
            backgroundColor = if (isSelected) Color.Blue else Color.Gray,
            contentColor = Color.White
        ),
        modifier = Modifier.padding(4.dp)
    ) {
        Text(componentName)
    }
}