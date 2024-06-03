package com.example.EvaluationMa.comanage

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ComponentManageScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var components by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            db.collection("components")
                .whereEqualTo("comanegerId", user.uid)
                .get()
                .addOnSuccessListener { result ->
                    components = result.documents.mapNotNull { document ->
                        document.data?.apply { put("uid", document.id) }
                    }
                }
                .addOnFailureListener { e ->
                    errorMessage = "Error fetching components: $e"
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Component Manager Dashboard", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("profile") }) {
            Text("Profile")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("Go to Component Detail", style = MaterialTheme.typography.h5)
        if (components.isNotEmpty()) {
            LazyColumn {
                items(components) { component ->
                    val componentId = component["uid"] as String
                    val componentName = component["name"] as String
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                navController.navigate("component_detail/$componentId")
                            },
                        elevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(componentName, style = MaterialTheme.typography.h6)
                        }
                    }
                }
            }
        } else {
            Text("No components found.")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Go to Component Announcement", style = MaterialTheme.typography.h5)
        if (components.isNotEmpty()) {
            LazyColumn {
                items(components) { component ->
                    val componentId = component["uid"] as String
                    val componentName = component["name"] as String
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                // 导航到ComponentAnnouncementScreen，传递componentId参数
                                navController.navigate("componentAnnouncement/$componentId")
                            },
                        elevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(componentName, style = MaterialTheme.typography.h6)
                        }
                    }
                }
            }
        } else {
            Text("No components found.")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colors.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}