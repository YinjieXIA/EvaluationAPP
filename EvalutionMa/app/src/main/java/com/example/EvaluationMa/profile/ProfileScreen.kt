package com.example.EvaluationMa.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.EvaluationMa.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@Composable
fun ProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }
    var isEditable by remember { mutableStateOf(false) }

    val currentUser = auth.currentUser

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    firstName = document.getString("firstName") ?: ""
                    lastName = document.getString("lastName") ?: ""
                    photoUrl = document.getString("photoUrl") ?: ""
                    userRole = document.getString("role") ?: ""
                    isEditable = true  // All users can edit information
                }
                .addOnFailureListener { e ->
                    errorMessage = "Error fetching user data: $e"
                }
        }
    }

    fun saveProfile() {
        val userMap = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "photoUrl" to photoUrl
        )
        db.collection("users").document(currentUser!!.uid).update(userMap)
            .addOnSuccessListener {
                errorMessage = "Profile updated successfully"
            }
            .addOnFailureListener { e ->
                errorMessage = "Error updating profile: $e"
            }
    }

    fun uploadImageToFirebaseStorage(uri: Uri, onSuccess: (String) -> Unit) {
        val storageRef = storage.reference
        val fileRef = storageRef.child("profile_images/${currentUser!!.uid}.png")
        val uploadTask = fileRef.putFile(uri)

        uploadTask.addOnSuccessListener {
            fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                onSuccess(downloadUri.toString())
            }
        }.addOnFailureListener { e ->
            errorMessage = "Error uploading image: $e"
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            uploadImageToFirebaseStorage(uri) { downloadUrl ->
                photoUrl = downloadUrl
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Profile", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditable
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditable
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text("Select Image")
            }
            Spacer(modifier = Modifier.width(16.dp))
            TextField(
                value = photoUrl,
                onValueChange = { photoUrl = it },
                label = { Text("Photo URL") },
                modifier = Modifier.weight(1f),
                enabled = isEditable
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = rememberAsyncImagePainter(
                model = photoUrl,
                error = painterResource(R.drawable.ic_broken_image)
            ),
            contentDescription = "Profile Photo",
            modifier = Modifier.size(128.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { saveProfile() }) {
            Text("Save")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
