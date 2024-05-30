package com.example.EvaluationMa.comanage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

data class StudentEvaluation(val studentId: String = "", val examName: String = "", val componentId: String = "", val evaluation: String = "", val timestamp: Long = 0)

@Composable
fun StudentEvaluationScreen(navController: NavController, componentId: String, examName: String, studentId: String) {
    val db = FirebaseFirestore.getInstance()
    var evaluation by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // 获取现有的评价
        db.collection("components").document(componentId).collection("evaluations")
            .whereEqualTo("studentId", studentId)
            .whereEqualTo("examName", examName)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    evaluation = result.documents[0].getString("evaluation") ?: ""
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching evaluation: $e"
            }
    }

    if (errorMessage.isNotEmpty()) {
        Text(errorMessage, color = Color.Red, modifier = Modifier.padding(16.dp))
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Evaluate Student", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = evaluation,
            onValueChange = { evaluation = it },
            label = { Text("Evaluation") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val newEvaluation = StudentEvaluation(studentId, examName, componentId, evaluation, System.currentTimeMillis())
            db.collection("components").document(componentId).collection("evaluations")
                .add(newEvaluation)
                .addOnSuccessListener {
                    successMessage = "Evaluation saved successfully"
                }
                .addOnFailureListener { e ->
                    errorMessage = "Error saving evaluation: $e"
                }
        }) {
            Text("Save Evaluation")
        }

        if (successMessage.isNotEmpty()) {
            Text(successMessage, color = Color.Green, modifier = Modifier.padding(16.dp))
        }
    }
}