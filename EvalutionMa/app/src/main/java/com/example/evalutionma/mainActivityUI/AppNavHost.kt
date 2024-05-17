package com.example.evalutionma.mainActivityUI

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.evalutionma.auth.LoginScreen
import com.example.evalutionma.auth.RegisterScreen
import com.example.evalutionma.auth.ForgotPasswordScreen
import com.example.evalutionma.admin.UserVerificationScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    var userRole by remember { mutableStateOf<String?>(null) }

    // 获取当前用户角色
    LaunchedEffect(auth.currentUser) {
        auth.currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    userRole = document.getString("role")
                }
        }
    }

    NavHost(navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("forgot_password") { ForgotPasswordScreen(navController) }
        composable("verify_users") {
            if (userRole == "module_manager") {
                UserVerificationScreen(navController)
            } else {
                NoPermissionScreen()
            }
        }
        // 添加其他页面
    }
}

@Composable
fun NoPermissionScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("You do not have permission to access this page.")
    }
}
