package com.example.evalutionma.auth

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // 登录成功，检查验证状态
                            val user = auth.currentUser
                            user?.let {
                                db.collection("users").document(it.uid).get()
                                    .addOnSuccessListener { document ->
                                        if (document != null && document.exists()) {
                                            val verified = document.getBoolean("verified") ?: false
                                            val role = document.getString("role") ?: ""
                                            if (verified) {
                                                if (role == "student") {
                                                    auth.signOut()
                                                    errorMessage = "Students do not have access to this app."
                                                } else {
                                                    errorMessage = ""
                                                    navController.navigate("verify_users")
                                                }
                                            } else {
                                                auth.signOut() // 登出用户
                                                errorMessage = "Account is not verified. Please contact the administrator."
                                            }
                                        } else {
                                            errorMessage = "No user data found. Please contact the administrator."
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        errorMessage = "Error checking verification status: ${e.message}"
                                    }
                            }
                        } else {
                            when (task.exception) {
                                is FirebaseAuthInvalidUserException -> {
                                    errorMessage = "Account does not exist."
                                }
                                is FirebaseAuthInvalidCredentialsException -> {
                                    errorMessage = "Incorrect password."
                                }
                                else -> {
                                    errorMessage = "Login failed: ${task.exception?.message}"
                                }
                            }
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
        TextButton(
            onClick = { navController.navigate("forgot_password") },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Forgot Password?")
        }
        TextButton(
            onClick = { navController.navigate("register") },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Register")
        }
    }
}
