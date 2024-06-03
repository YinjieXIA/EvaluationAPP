package com.example.EvaluationStu.dashboard

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.EvaluationStu.models.Announcement
import com.google.firebase.firestore.FieldPath
import kotlinx.coroutines.tasks.await

@Composable
fun StudentHomeScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var studentName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var performanceSummary by remember { mutableStateOf("Loading...") }
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var components by remember { mutableStateOf<List<Triple<String, String, Double>>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    var moduleManagerName by remember { mutableStateOf("") }
    var moduleManagerEmail by remember { mutableStateOf("") }
    val context = LocalContext.current

    // 获取模块管理员信息
    fun fetchModuleManager() {
        db.collection("users").whereEqualTo("role", "module_manager").get()
            .addOnSuccessListener { result ->
                val manager = result.documents.firstOrNull()
                if (manager != null) {
                    moduleManagerName = manager.getString("firstName") + " " + manager.getString("lastName")
                    moduleManagerEmail = manager.getString("email") ?: ""
                }
            }
    }

    // 获取学生信息
    LaunchedEffect(Unit) {
        fetchModuleManager()
        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    studentName = document.getString("firstName") ?: "Student"
                    lastName = document.getString("lastName") ?: ""
                    val studentGroupId = document.getString("group") ?: ""
                    Log.d("StudentHomeScreen", "Student group ID: $studentGroupId")
                    if (studentGroupId.isNotEmpty()) {
                        fetchAnnouncements(db, studentGroupId) { result ->
                            announcements = result
                        }
                        fetchComponents(db, studentGroupId) { result ->
                            components = result
                            fetchPerformanceSummary(db, user.uid, result) { summary ->
                                performanceSummary = summary
                            }
                        }
                    } else {
                        Log.e("StudentHomeScreen", "Group ID not found in user document")
                        errorMessage = "You have not been assigned a group. Please contact a module manager."
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("StudentHomeScreen", "Failed to fetch user document", exception)
                    errorMessage = "Unable to fetch user information. Please contact a module manager."
                }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Welcome back, $studentName $lastName",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Current Semester Performance", style = MaterialTheme.typography.h6)
                    Text(text = performanceSummary, style = MaterialTheme.typography.body1)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("scores") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Icon(Icons.Default.Grade, contentDescription = "View Grades", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Scores")
            }

            Button(
                onClick = { navController.navigate("announcements") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Icon(Icons.Default.Announcement, contentDescription = "View Announcements", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Announcements")
            }

            Button(
                onClick = { navController.navigate("team_member_scores") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Icon(Icons.Default.Group, contentDescription = "Team", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Team")
            }

            Button(
                onClick = { navController.navigate("student_profile") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Profile")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(16.dp)
                )
                if (moduleManagerEmail.isNotEmpty()) {
                    TextButton(onClick = {
                        val emailIntent = Intent(
                            Intent.ACTION_SENDTO, Uri.fromParts("mailto", moduleManagerEmail, null)
                        ).apply {
                            putExtra(Intent.EXTRA_SUBJECT, "Request for Group/Team Assignment")
                            putExtra(Intent.EXTRA_TEXT, "Dear $moduleManagerName,\n\nI have not been assigned a group/team/component. Please assist me with the assignment.\n\nBest regards,\n$studentName $lastName")
                        }
                        context.startActivity(Intent.createChooser(emailIntent, "Send email..."))
                    }) {
                        Text(text = "Contact Module Manager: $moduleManagerName")
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Announcements Summary", style = MaterialTheme.typography.h6)
                        announcements.forEach { announcement ->
                            Text(text = announcement.title, style = MaterialTheme.typography.body1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Components and Weights", style = MaterialTheme.typography.h6)
                        components.forEach { (id, component, weight) ->
                            Text(text = "$component: $weight", style = MaterialTheme.typography.body1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Scoring Standards", style = MaterialTheme.typography.h6)
                        val standards = listOf(
                            "Not Acquired" to "0",
                            "Far" to "7",
                            "Close" to "10",
                            "Very Close" to "13",
                            "Expected" to "16",
                            "Beyond" to "20",
                            "Missing Evaluation" to "-",
                            "Not Applicable" to "-"
                        )
                        standards.forEach { (level, score) ->
                            Text(text = "$level: $score", style = MaterialTheme.typography.body1)
                        }
                    }
                }
            }
        }
    }
}

fun fetchAnnouncements(db: FirebaseFirestore, studentGroup: String, callback: (List<Announcement>) -> Unit) {
    db.collection("announcements")
        .whereEqualTo("groupId", studentGroup)  // Targeted announcements for the student's group
        .get()
        .addOnSuccessListener { targetedResult ->
            val targetedAnnouncements = targetedResult.documents.mapNotNull { it.toObject(Announcement::class.java)?.copy(id = it.id) }
            db.collection("announcements")
                .whereEqualTo("groupId", null)  // General announcements for all students
                .get()
                .addOnSuccessListener { generalResult ->
                    val generalAnnouncements = generalResult.documents.mapNotNull { it.toObject(Announcement::class.java)?.copy(id = it.id) }
                    callback(targetedAnnouncements + generalAnnouncements)  // Combine both lists of announcements
                }
        }
}

fun fetchComponents(db: FirebaseFirestore, studentGroup: String, callback: (List<Triple<String, String, Double>>) -> Unit) {
    db.collection("groups").document(studentGroup).get()
        .addOnSuccessListener { document ->
            val components = document.get("components") as? List<String> ?: emptyList()
            if (components.isNotEmpty()) {
                db.collection("components").whereIn(FieldPath.documentId(), components).get()
                    .addOnSuccessListener { result ->
                        val componentList = result.documents.mapNotNull { doc ->
                            val name = doc.getString("name") ?: "Unknown Component"
                            val weight = doc.getDouble("weight") ?: 0.0
                            val id = doc.id
                            Log.d("ComponentFetch", "Component: $name, Weight: $weight, ID: $id")
                            Triple(id, name, weight)
                        }
                        callback(componentList)
                    }
            } else {
                callback(emptyList())
            }
        }
}

fun fetchPerformanceSummary(
    db: FirebaseFirestore,
    userId: String,
    components: List<Triple<String, String, Double>>,
    callback: (String) -> Unit
) {
    db.collection("users").document(userId).get()
        .addOnSuccessListener { document ->
            val teamId = document.getString("team") ?: ""
            if (teamId.isNotEmpty()) {
                db.collection("groups").document(document.getString("group")!!).collection("teams").document(teamId).collection("students").document(userId).collection("totalScores").get()
                    .addOnSuccessListener { scoreResult ->
                        val scores = scoreResult.documents.mapNotNull { scoreDoc ->
                            val componentId = scoreDoc.getString("componentId")
                            val totalScore = scoreDoc.getDouble("totalScore")
                            Log.d("ScoreFetch", "Component ID: $componentId, Score: $totalScore")
                            componentId to totalScore
                        }.toMap()

                        Log.d("PerformanceSummary", "Scores fetched: $scores")

                        var weightedSum = 0.0
                        var totalWeight = 0.0

                        components.forEach { (componentId, componentName, weight) ->
                            val score = scores[componentId] ?: 0.0
                            Log.d("PerformanceSummary", "Component ID: $componentId, Weight: $weight, Score: $score")
                            weightedSum += score * weight
                            totalWeight += weight
                        }

                        val averageScore = if (totalWeight > 0) weightedSum / totalWeight else 0.0
                        val performanceDetails = "Semester average score: %.2f / 20".format(averageScore)

                        callback(performanceDetails)
                    }
                    .addOnFailureListener { exception ->
                        callback("Failed to fetch scores: ${exception.message}")
                    }
            } else {
                callback("No team assigned.")
            }
        }
        .addOnFailureListener { exception ->
            callback("Failed to fetch user document: ${exception.message}")
        }
}
