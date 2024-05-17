package com.example.evaluationstu

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.evaluationstu.auth.LoginScreen
import com.example.evaluationstu.auth.RegisterScreen
import com.example.evaluationstu.auth.ForgotPasswordScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("forgot_password") { ForgotPasswordScreen(navController) }
        composable("student_home") { StudentHomeScreen(navController) } // 学生端主页
    }
}

@Composable
fun StudentHomeScreen(navController: NavController) {
    // 学生端主页实现
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to the Student Home Page!")
    }
}
