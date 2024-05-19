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
import androidx.compose.ui.graphics.Color

data class Skill(val id: String = "", val name: String = "", val description: String = "")

@Composable
fun SkillManagementScreen(navController: NavController, componentId: String) {
    val db = FirebaseFirestore.getInstance()
    var skills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var skillName by remember { mutableStateOf("") }
    var skillDescription by remember { mutableStateOf("") }
    var selectedSkill by remember { mutableStateOf<Skill?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("components").document(componentId).collection("skills").get()
            .addOnSuccessListener { result ->
                skills = result.documents.mapNotNull { document ->
                    document.toObject(Skill::class.java)?.copy(id = document.id)
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching skills: $e"
                Log.e("SkillManagementScreen", errorMessage)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Skill Management", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = skillName,
            onValueChange = { skillName = it },
            label = { Text("Skill Name") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = skillDescription,
            onValueChange = { skillDescription = it },
            label = { Text("Skill Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                val newSkill = Skill(name = skillName, description = skillDescription)
                db.collection("components").document(componentId).collection("skills").add(newSkill)
                    .addOnSuccessListener { documentReference ->
                        newSkill.copy(id = documentReference.id)
                        skills = skills + newSkill
                        skillName = ""
                        skillDescription = ""
                    }
                    .addOnFailureListener { e ->
                        errorMessage = "Error adding skill: $e"
                        Log.e("SkillManagementScreen", errorMessage)
                    }
            }) {
                Text("Add Skill")
            }
            Button(onClick = {
                selectedSkill?.let { skill ->
                    val updatedSkill = skill.copy(name = skillName, description = skillDescription)
                    db.collection("components").document(componentId).collection("skills").document(skill.id).set(updatedSkill)
                        .addOnSuccessListener {
                            skills = skills.map {
                                if (it.id == skill.id) updatedSkill else it
                            }
                            skillName = ""
                            skillDescription = ""
                            selectedSkill = null
                        }
                        .addOnFailureListener { e ->
                            errorMessage = "Error updating skill: $e"
                            Log.e("SkillManagementScreen", errorMessage)
                        }
                }
            }, enabled = selectedSkill != null) {
                Text("Update Skill")
            }
        }

        LazyColumn {
            items(skills) { skill ->
                SkillItem(skill, onEdit = { selectedSkill = it; skillName = it.name; skillDescription = it.description }, onDelete = {
                    db.collection("components").document(componentId).collection("skills").document(skill.id).delete()
                        .addOnSuccessListener {
                            skills = skills.filter { it.id != skill.id }
                        }
                        .addOnFailureListener { e ->
                            errorMessage = "Error deleting skill: $e"
                            Log.e("SkillManagementScreen", errorMessage)
                        }
                })
            }
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun SkillItem(skill: Skill, onEdit: (Skill) -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(skill.name, style = MaterialTheme.typography.body1)
                Text(skill.description, style = MaterialTheme.typography.body2)
            }
            Row {
                Button(onClick = { onEdit(skill) }) {
                    Text("Edit")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onDelete() }) {
                    Text("Delete")
                }
            }
        }
    }
}