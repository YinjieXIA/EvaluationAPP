package com.example.EvaluationMa.comanage.addScore

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.EvaluationMa.comanage.Skill
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun StudentScoreListScreen(navController: NavController, groupId: String, teamId: String, componentId: String) {
    val db = FirebaseFirestore.getInstance()
    var skills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(componentId) {
        loadSkills(componentId, onSuccess = { result ->
            skills = result
        }, onFailure = { e ->
            errorMessage = "Error fetching skills: $e"
            Log.e("StudentScoreListScreen", errorMessage)
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Skills in Component", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (skills.isNotEmpty()) {
            LazyColumn {
                items(skills) { skill ->
                    SkillItem(
                        skill = skill,
                        onSkillSelected = {
                            navController.navigate("student_scores/${groupId}/${teamId}/${componentId}/${skill.id}")
                        }
                    )
                }
            }
        } else {
            Text("No skills found for this component.")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun SkillItem(skill: Skill, onSkillSelected: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onSkillSelected() },
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(skill.title, style = MaterialTheme.typography.body1)
        }
    }
}

fun loadSkills(
    componentId: String,
    onSuccess: (List<Skill>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    db.collection("components").document(componentId).collection("skills")
        .get()
        .addOnSuccessListener { result ->
            val skills = result.documents.mapNotNull { document ->
                document.toObject(Skill::class.java)?.copy(id = document.id)
            }
            onSuccess(skills)
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}

@Composable
fun StudentScoreItem(studentName: String, studentScore: StudentScore) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(studentName, style = MaterialTheme.typography.body1)
        Text(studentScore.score.toString(), style = MaterialTheme.typography.body1)
    }
}

fun loadStudentScores(
    groupId: String,
    teamId: String,
    studentId: String,
    onSuccess: (Map<String, Double>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    db.collection("groups").document(groupId)
        .collection("teams").document(teamId)
        .collection("students").document(studentId)
        .collection("studentScores").get()
        .addOnSuccessListener { result ->
            val scores = result.documents.associate { document ->
                val skillId = document.id
                val score = document.getDouble("score") ?: 0.0
                skillId to score
            }
            onSuccess(scores)
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}