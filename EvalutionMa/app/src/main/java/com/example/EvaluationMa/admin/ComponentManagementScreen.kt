package com.example.EvaluationMa.admin

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color

data class Component(val id: String = "", val name: String = "", val description: String = "", val comanegerId: String = "")
data class User(val uid: String = "", val email: String = "", val role: String = "", val verified: Boolean = false)

@Composable
fun ComponentManagementScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var components by remember { mutableStateOf<List<Component>>(emptyList()) }
    var comanegers by remember { mutableStateOf<List<User>>(emptyList()) }
    var componentName by remember { mutableStateOf("") }
    var componentDescription by remember { mutableStateOf("") }
    var selectedComponent by remember { mutableStateOf<Component?>(null) }
    var selectedComaneger by remember { mutableStateOf<User?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("components").get()
            .addOnSuccessListener { result ->
                components = result.documents.mapNotNull { document ->
                    document.toObject(Component::class.java)?.copy(id = document.id)
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching components: $e"
                Log.e("ComponentManagementScreen", errorMessage)
            }
        // 获取教师列表
        db.collection("users").whereEqualTo("role", "component_manager").whereEqualTo("verified", true).get()
            .addOnSuccessListener { result ->
                comanegers = result.documents.mapNotNull { document ->
                    document.toObject(User::class.java)?.copy(uid = document.id)
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching comanegers: $e"
                Log.e("ComponentManagementScreen", errorMessage)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Component Management", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = componentName,
            onValueChange = { componentName = it },
            label = { Text("Component Name") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = componentDescription,
            onValueChange = { componentDescription = it },
            label = { Text("Component Description") },
            modifier = Modifier.fillMaxWidth()
        )

        // 教师选择
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(selectedComaneger?.email ?: "Select a Component Manager", modifier = Modifier.fillMaxWidth().clickable { expanded = true })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                comanegers.forEach { comaneger ->
                    DropdownMenuItem(onClick = {
                        selectedComaneger = comaneger
                        expanded = false
                    }) {
                        Text(comaneger.email)
                    }
                }
            }
        }


        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                val newComponent = Component(name = componentName, description = componentDescription, comanegerId = selectedComaneger?.uid ?: "")
                db.collection("components").add(newComponent)
                    .addOnSuccessListener { documentReference ->
                        newComponent.copy(id = documentReference.id)
                        components = components + newComponent
                        componentName = ""
                        componentDescription = ""
                        selectedComaneger = null
                    }
                    .addOnFailureListener { e ->
                        errorMessage = "Error adding component: $e"
                        Log.e("ComponentManagementScreen", errorMessage)
                    }
            }) {
                Text("Add Component")
            }
            Button(onClick = {
                selectedComponent?.let { component ->
                    val updatedComponent = component.copy(
                        name = componentName,
                        description = componentDescription,
                        comanegerId = selectedComaneger?.uid ?: component.comanegerId
                    )
                    db.collection("components").document(component.id).set(updatedComponent)
                        .addOnSuccessListener {
                            components = components.map {
                                if (it.id == component.id) updatedComponent else it
                            }
                            componentName = ""
                            componentDescription = ""
                            selectedComponent = null
                            selectedComaneger = null
                        }
                        .addOnFailureListener { e ->
                            errorMessage = "Error updating component: $e"
                            Log.e("ComponentManagementScreen", errorMessage)
                        }
                }
            }, enabled = selectedComponent != null) {
                Text("Update Component")
            }
        }

        LazyColumn {
            items(components) { component ->
                ComponentItem(component, onEdit = {
                    selectedComponent = it
                    componentName = it.name
                    componentDescription = it.description
                    selectedComaneger = comanegers.find { comaneger -> comaneger.uid == it.comanegerId }
                }, onDelete = {
                    db.collection("components").document(component.id).delete()
                        .addOnSuccessListener {
                            components = components.filter { it.id != component.id }
                        }
                        .addOnFailureListener { e ->
                            errorMessage = "Error deleting component: $e"
                            Log.e("ComponentManagementScreen", errorMessage)
                        }
                }, onManageSkills = {
                    navController.navigate("manage_skills/${component.id}")
                })
            }
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun ComponentItem(component: Component, onEdit: (Component) -> Unit, onDelete: () -> Unit, onManageSkills: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(component.name, style = MaterialTheme.typography.body1)
            Text(component.description, style = MaterialTheme.typography.body2)
            Text("Component Manager ID: ${component.comanegerId}", style = MaterialTheme.typography.body2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { onEdit(component) }) {
                    Text("Edit")
                }
                Button(onClick = { onDelete() }) {
                    Text("Delete")
                }
                Button(onClick = { onManageSkills() }) {
                    Text("Manage Skills")
                }
            }
        }
    }
}