package com.example.EvaluationMa.comanage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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

@Composable
fun ExamListScreen(navController: NavController, componentId: String) {
    val db = FirebaseFirestore.getInstance()
    var exams by remember { mutableStateOf<List<String>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // 获取考试列表
        db.collection("components").document(componentId).collection("studentScores").get()
            .addOnSuccessListener { result ->
                exams = result.documents.mapNotNull { it.getString("examName") }.distinct()
            }
            .addOnFailureListener { e ->
                errorMessage = "Error fetching exams: $e"
            }
    }

    if (errorMessage.isNotEmpty()) {
        Text(errorMessage, color = Color.Red, modifier = Modifier.padding(16.dp))
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Exams", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(exams) { exam ->
                Button(onClick = {
                    navController.navigate("exam_scores/$componentId/$exam")
                }) {
                    Text(exam)
                }
            }
        }
    }
}