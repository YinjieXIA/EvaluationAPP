package com.example.EvaluationMa.admin

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ReEvaluationRequest(
    val userId: String = "",
    val skillName: String = "",
    val reason: String = "",
    val fileUrl: String? = null,
    val timestamp: Long = 0
)
data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = ""
)

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AdminReviewRequestsScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    var requests by remember { mutableStateOf<List<ReEvaluationRequest>>(emptyList()) }
    var userMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            // Fetch re-evaluation requests
            val requestResult = db.collection("re-evaluation_requests").get().await()
            val fetchedRequests = requestResult.documents.mapNotNull { document ->
                document.toObject(ReEvaluationRequest::class.java)
            }
            requests = fetchedRequests

            // Fetch user details for each request
            val userIds = fetchedRequests.map { it.userId }.distinct()
            val userMapResult = mutableMapOf<String, User>()
            userIds.forEach { userId ->
                val userDocument = db.collection("users").document(userId).get().await()
                val user = userDocument.toObject(User::class.java)
                if (user != null) {
                    userMapResult[userId] = user
                }
            }
            userMap = userMapResult
        } catch (e: Exception) {
            errorMessage = "Error fetching requests or user data: $e"
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text("Review Re-evaluation Requests") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("Re-evaluation Requests", style = MaterialTheme.typography.h5)
                Spacer(modifier = Modifier.height(16.dp))

                if (requests.isNotEmpty()) {
                    LazyColumn {
                        items(requests) { request ->
                            val user = userMap[request.userId]
                            val userName = user?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown User"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                elevation = 4.dp
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Student: $userName", style = MaterialTheme.typography.h6)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Skill: ${request.skillName}", style = MaterialTheme.typography.h6)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Reason: ${request.reason}", style = MaterialTheme.typography.body1)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    request.fileUrl?.let { fileUrl ->
                                        TextButton(onClick = {
                                            coroutineScope.launch {
                                                try {
                                                    // Implement your file download logic here
                                                    val uri = storage.getReferenceFromUrl(fileUrl).downloadUrl.await()
                                                    successMessage = "File ready to download: $uri"
                                                    scaffoldState.snackbarHostState.showSnackbar("File ready to download")
                                                } catch (e: Exception) {
                                                    errorMessage = "Error downloading file: $e"
                                                    scaffoldState.snackbarHostState.showSnackbar("Error downloading file")
                                                }
                                            }
                                        }) {
                                            Text("Download Supporting File", color = MaterialTheme.colors.primary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Submitted at: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(request.timestamp))}", style = MaterialTheme.typography.body2)
                                }
                            }
                        }
                    }
                } else {
                    Text("No re-evaluation requests found.")
                }

                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                }

                if (successMessage.isNotEmpty()) {
                    Text(successMessage, color = Color.Green, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    )
}