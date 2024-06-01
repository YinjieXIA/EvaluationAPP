package com.example.EvaluationMa.admin

import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ModuleManageScreen(navController: NavController) {
    val adminOptions = listOf("Verify User Registrations", "Manage Components","Manage Group team","Manage Student","Announcement Management","Profile")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Admin Dashboard", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(adminOptions) { option ->
                AdminOptionItem(option, onOptionSelected = {
                    when (option) {
                        "Verify User Registrations" -> navController.navigate("verify_users")
                        "Manage Components" -> navController.navigate("manage_components")
                        "Manage Group team" -> navController.navigate("group_team_management")
                        "Manage Student" -> navController.navigate("student_management")
                        "Announcement Management" -> navController.navigate("announcement_screen")
                        "Profile" -> navController.navigate("profile")

                    }
                })
            }
        }
    }
}

@Composable
fun AdminOptionItem(option: String, onOptionSelected: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onOptionSelected() },
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(option, style = MaterialTheme.typography.body1)
        }
    }
}