package com.example.EvaluationMa.comanage

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import androidx.compose.ui.graphics.Color

data class Skill(val id: String = "", val title: String = "", val description: String = "", val link: String = "")

@Composable
fun SkillManagementScreen(navController: NavController, componentId: String) {
    val db = FirebaseFirestore.getInstance()
    var skills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var skillTitle by remember { mutableStateOf("") }
    var skillDescription by remember { mutableStateOf("") }
    var skillLink by remember { mutableStateOf("") }
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
            value = skillTitle,
            onValueChange = { skillTitle = it },
            label = { Text("Skill Title") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = skillDescription,
            onValueChange = { skillDescription = it },
            label = { Text("Skill Description") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = skillLink,
            onValueChange = { skillLink = it },
            label = { Text("Skill Link") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                val newSkill = Skill(title = skillTitle, description = skillDescription, link = skillLink)
                db.collection("components").document(componentId).collection("skills").add(newSkill)
                    .addOnSuccessListener { documentReference ->
                        newSkill.copy(id = documentReference.id)
                        skills = skills + newSkill
                        skillTitle = ""
                        skillDescription = ""
                        skillLink = ""
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
                    val updatedSkill = skill.copy(title = skillTitle, description = skillDescription, link = skillLink)
                    db.collection("components").document(componentId).collection("skills").document(skill.id).set(updatedSkill)
                        .addOnSuccessListener {
                            skills = skills.map {
                                if (it.id == skill.id) updatedSkill else it
                            }
                            skillTitle = ""
                            skillDescription = ""
                            skillLink = ""
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
                SkillItem(skill, onEdit = {
                    selectedSkill = it
                    skillTitle = it.title
                    skillDescription = it.description
                    skillLink = it.link
                }, onDelete = {
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
        Column(modifier = Modifier.padding(16.dp)) {
            Text(skill.title, style = MaterialTheme.typography.body1)
            Text(skill.description, style = MaterialTheme.typography.body2)
            Text(skill.link, style = MaterialTheme.typography.body2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { onEdit(skill) }) {
                    Text("Edit")
                }
                Button(onClick = { onDelete() }) {
                    Text("Delete")
                }
            }
        }
    }
}