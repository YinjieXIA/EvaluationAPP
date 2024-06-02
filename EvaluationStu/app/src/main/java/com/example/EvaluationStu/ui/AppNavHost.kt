package com.example.EvaluationStu.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.EvaluationStu.announcements.AnnouncementDetailScreen
import com.example.EvaluationStu.announcements.AnnouncementsScreen
import com.example.EvaluationStu.auth.LoginScreen
import com.example.EvaluationStu.auth.RegisterScreen
import com.example.EvaluationStu.auth.ForgotPasswordScreen
import com.example.EvaluationStu.profile.StudentProfileScreen
import com.example.EvaluationStu.dashboard.StudentHomeScreen
import com.example.EvaluationStu.profile.ChangePasswordScreen
import com.example.EvaluationStu.scores.ScoresScreen
import com.example.EvaluationStu.scores.SkillDetailScreen
import com.example.EvaluationStu.scores.ScoreHistoryScreen
import com.example.EvaluationStu.scores.RequestReviewScreen
import com.example.EvaluationStu.team.TeamMemberDetailScreen
import com.example.EvaluationStu.team.TeamMemberScoresScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("forgot_password") { ForgotPasswordScreen(navController) }
        composable("student_home") { StudentHomeScreen(navController) } // 学生端主页
        composable("student_profile") { StudentProfileScreen(navController) }
        composable("change_password") { ChangePasswordScreen(navController = navController) }
        composable("scores") { ScoresScreen(navController) }
        composable("score_detail/{componentId}") { backStackEntry ->
            val componentId = backStackEntry.arguments?.getString("componentId") ?: ""
            SkillDetailScreen(navController, componentId)
        }
        composable("score_history/{skillName}") { backStackEntry ->
            val skillName = backStackEntry.arguments?.getString("skillName") ?: ""
            ScoreHistoryScreen(navController, skillName)
        }
        composable("request_review/{skillName}") { backStackEntry ->
            val skillName = backStackEntry.arguments?.getString("skillName") ?: ""
            RequestReviewScreen(navController, skillName)
        }
        composable("announcements") { AnnouncementsScreen(navController) }
        composable("announcement_detail/{announcementId}") { backStackEntry ->
            val announcementId = backStackEntry.arguments?.getString("announcementId") ?: return@composable
            AnnouncementDetailScreen(navController, announcementId)
        }
        composable("team_member_scores") { TeamMemberScoresScreen(navController) }
        composable("team_member_detail/{memberId}") { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId") ?: ""
            TeamMemberDetailScreen(navController, memberId)
        }
    }
}
