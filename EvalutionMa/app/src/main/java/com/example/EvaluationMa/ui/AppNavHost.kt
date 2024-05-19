package com.example.EvaluationMa.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.EvaluationMa.admin.ComponentManagementScreen
import com.example.EvaluationMa.admin.ModuleManageScreen
import com.example.EvaluationMa.admin.SkillManagementScreen
import com.example.EvaluationMa.auth.LoginScreen
import com.example.EvaluationMa.auth.RegisterScreen
import com.example.EvaluationMa.auth.ForgotPasswordScreen
import com.example.EvaluationMa.admin.UserVerificationScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
//                    Log.d("AppNavHost", "User role: $userRole")
                }
//                .addOnFailureListener { exception ->
//                    Log.e("AppNavHost", "Error getting user role: $exception")
//                }
        }
    }

    NavHost(navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("forgot_password") { ForgotPasswordScreen(navController) }
//        composable("verify_users") {
//            if (userRole == "module_manager") {
//                UserVerificationScreen(navController)
//            } else {
//                NoPermissionScreen()
//            }
//        }
        composable("verify_users") { UserVerificationScreen(navController) }
        composable("module_manage") { ModuleManageScreen(navController) }
        composable("manage_components") { ComponentManagementScreen(navController) }
        composable(
            route = "manage_skills/{componentId}",
            arguments = listOf(navArgument("componentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val componentId = backStackEntry.arguments?.getString("componentId") ?: ""
            SkillManagementScreen(navController, componentId)
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
