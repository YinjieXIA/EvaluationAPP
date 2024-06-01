package com.example.EvaluationStu.scores

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun RequestReviewScreen(navController: NavController, skillName: String) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val currentUser = auth.currentUser
    var requestReason by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var uploadStatus by remember { mutableStateOf<UploadStatus?>(null) }
    var requestStatus by remember { mutableStateOf<RequestStatus?>(null) }

    // 文件选择器的Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
    }

    // Snackbar的状态
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text("Request Re-evaluation for $skillName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    Text(
                        text = "Skill: $skillName",
                        style = MaterialTheme.typography.h5,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Please enter the reason for re-evaluation:",
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    BasicTextField(
                        value = requestReason,
                        onValueChange = { requestReason = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(8.dp)
                            .border(1.dp, MaterialTheme.colors.onSurface, shape = MaterialTheme.shapes.small)
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 上传文件的按钮和状态显示
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                filePickerLauncher.launch("*/*")
                            }
                        ) {
                            Text("Upload Supporting File")
                        }

                        selectedFileUri?.let {
                            Text(text = "Selected file: ${it.path}", style = MaterialTheme.typography.body2)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                currentUser?.let { user ->
                                    coroutineScope.launch {
                                        try {
                                            // 上传文件到Firebase Storage
                                            selectedFileUri?.let { uri ->
                                                uploadStatus = UploadStatus.UPLOADING
                                                val fileName = UUID.randomUUID().toString()
                                                val ref = storage.reference.child("re-evaluation_files/$fileName")
                                                ref.putFile(uri).await()
                                                val downloadUrl = ref.downloadUrl.await()

                                                // 提交重新评估请求到服务器
                                                val request = hashMapOf(
                                                    "userId" to user.uid,
                                                    "skillName" to skillName,
                                                    "reason" to requestReason,
                                                    "fileUrl" to downloadUrl.toString(),
                                                    "timestamp" to System.currentTimeMillis()
                                                )
                                                db.collection("re-evaluation_requests").add(request).await()
                                                requestStatus = RequestStatus.SUCCESS
                                                uploadStatus = UploadStatus.SUCCESS
                                                scaffoldState.snackbarHostState.showSnackbar("Request submitted successfully.")
                                            } ?: run {
                                                // 没有文件选择时直接提交请求
                                                val request = hashMapOf(
                                                    "userId" to user.uid,
                                                    "skillName" to skillName,
                                                    "reason" to requestReason,
                                                    "timestamp" to System.currentTimeMillis()
                                                )
                                                db.collection("re-evaluation_requests").add(request).await()
                                                requestStatus = RequestStatus.SUCCESS
                                                scaffoldState.snackbarHostState.showSnackbar("Request submitted successfully.")
                                            }
                                        } catch (e: Exception) {
                                            requestStatus = RequestStatus.FAILURE
                                            uploadStatus = UploadStatus.FAILURE
                                            scaffoldState.snackbarHostState.showSnackbar("Failed to submit the request. Please try again.")
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Submit Request")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    requestStatus?.let { status ->
                        Text(
                            text = when (status) {
                                RequestStatus.SUCCESS -> "Request submitted successfully."
                                RequestStatus.FAILURE -> "Failed to submit the request. Please try again."
                            },
                            color = if (status == RequestStatus.SUCCESS) MaterialTheme.colors.primary else MaterialTheme.colors.error,
                            style = MaterialTheme.typography.body1
                        )
                    }

                    uploadStatus?.let { status ->
                        Text(
                            text = when (status) {
                                UploadStatus.UPLOADING -> "Uploading file..."
                                UploadStatus.SUCCESS -> "File uploaded successfully."
                                UploadStatus.FAILURE -> "Failed to upload file. Please try again."
                            },
                            color = if (status == UploadStatus.SUCCESS) MaterialTheme.colors.primary else MaterialTheme.colors.error,
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    )
}

enum class RequestStatus {
    SUCCESS,
    FAILURE
}

enum class UploadStatus {
    UPLOADING,
    SUCCESS,
    FAILURE
}
