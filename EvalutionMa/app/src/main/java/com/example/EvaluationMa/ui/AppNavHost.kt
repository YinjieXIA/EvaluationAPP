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
import com.example.EvaluationMa.comanage.ComponentManagementScreen
import com.example.EvaluationMa.admin.ModuleManageScreen
import com.example.EvaluationMa.comanage.SkillManagementScreen
import com.example.EvaluationMa.auth.LoginScreen
import com.example.EvaluationMa.auth.RegisterScreen
import com.example.EvaluationMa.auth.ForgotPasswordScreen
import com.example.EvaluationMa.admin.UserVerificationScreen
import com.example.EvaluationMa.admin.studentManage.AddGroupScreen
import com.example.EvaluationMa.admin.studentManage.AddStudentScreen
import com.example.EvaluationMa.admin.studentManage.AddTeamScreen
import com.example.EvaluationMa.admin.studentManage.GroupTeamManagementScreen
import com.example.EvaluationMa.admin.studentManage.StudentDetailScreen
import com.example.EvaluationMa.admin.studentManage.StudentManagementScreen
import com.example.EvaluationMa.admin.studentManage.AssignClientScreen
import com.example.EvaluationMa.admin.studentManage.AssignComponentScreen
import com.example.EvaluationMa.admin.studentManage.AssignTutorScreen
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
        composable("group_team_management") {
            if (userRole == "module_manager") {
                GroupTeamManagementScreen(navController)
            } else {
                NoPermissionScreen()
            }
        }
        composable("student_management") {
            if (userRole == "module_manager" || userRole == "component_manager") {
                StudentManagementScreen(navController)
            } else {
                NoPermissionScreen()
            }
        }
        composable("student_detail/{studentId}") { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            if (userRole == "module_manager" || userRole == "component_manager") {
                StudentDetailScreen(navController, studentId)
            } else {
                NoPermissionScreen()
            }
        }
        composable("add_group") {
            if (userRole == "module_manager") {
                AddGroupScreen(navController)
            } else {
                NoPermissionScreen()
            }
        }
        composable("add_team/{groupId}") { backStackEntry ->
            if (userRole == "module_manager") {
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                AddTeamScreen(navController, groupId)
            } else {
                NoPermissionScreen()
            }
        }
        composable(
            route = "add_student/{groupId}/{teamId}",
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("teamId") { type = NavType.StringType }
            )
        ) {backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
            AddStudentScreen(navController, groupId, teamId)
        }
        composable(
            route = "assign_client/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            AssignClientScreen(navController, groupId)
        }
        composable(
            route = "assign_component/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            AssignComponentScreen(navController, groupId)
        }
        composable(
            route = "assign_tutor/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            AssignTutorScreen(navController, groupId)
        }
        // 添加其他
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
