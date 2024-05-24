package com.example.EvaluationMa.comanage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MoreAnnouncementsScreen(navController: NavController, componentId: String) {
    val db = FirebaseFirestore.getInstance()
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    var lastVisibleAnnouncement by remember { mutableStateOf<DocumentSnapshot?>(null) }
    val pageSize = 5

    LaunchedEffect(Unit) {
        loadMoreAnnouncements(db, componentId, pageSize, null, onSuccess = { result, lastVisible ->
            announcements = result
            lastVisibleAnnouncement = lastVisible
        }, onFailure = { e ->
            errorMessage = "Error fetching announcements: $e"
        })
    }

    if (errorMessage.isNotEmpty()) {
        Text(errorMessage, color = Color.Red, modifier = Modifier.padding(16.dp))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Announcements", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(announcements) { announcement ->
                AnnouncementItem(announcement)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            loadMoreAnnouncements(db, componentId, pageSize, lastVisibleAnnouncement, onSuccess = { result, lastVisible ->
                announcements = announcements + result
                lastVisibleAnnouncement = lastVisible
            }, onFailure = { e ->
                errorMessage = "Error fetching more announcements: $e"
            })
        }) {
            Text("Load More")
        }
    }
}

fun loadMoreAnnouncements(
    db: FirebaseFirestore,
    componentId: String,
    pageSize: Int,
    lastVisible: DocumentSnapshot?,
    onSuccess: (List<Announcement>, DocumentSnapshot?) -> Unit,
    onFailure: (Exception) -> Unit
) {
    var query = db.collection("components").document(componentId).collection("announcements")
        .orderBy("timestamp")
        .limit(pageSize.toLong())

    if (lastVisible != null) {
        query = query.startAfter(lastVisible)
    }

    query.get()
        .addOnSuccessListener { result ->
            val announcements = result.documents.mapNotNull { document ->
                document.toObject(Announcement::class.java)?.copy(id = document.id)
            }
            val lastVisible = result.documents.lastOrNull()
            onSuccess(announcements, lastVisible)
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}