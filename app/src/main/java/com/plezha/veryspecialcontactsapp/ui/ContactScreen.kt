package com.plezha.veryspecialcontactsapp.ui

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ContactScreen(viewModel: ContactsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
//    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val contactPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
    )

    LaunchedEffect(contactPermissionsState.allPermissionsGranted) {
        if (contactPermissionsState.allPermissionsGranted) {
            viewModel.loadContacts()
        }
    }
    LaunchedEffect(uiState.deduplicationMessage) {
        val message = uiState.deduplicationMessage

        println("deduplicationMessage $message")

        if (message != null) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
    }
    LaunchedEffect(uiState.error) {
        println("error ${uiState.error}")
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = "Error: $it",
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (contactPermissionsState.allPermissionsGranted) {
                Button(
                    onClick = viewModel::deleteAllDuplicates,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = !uiState.isLoading
                ) {
                    Text("Delete Duplicate Contacts")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!contactPermissionsState.allPermissionsGranted) {
                PermissionRequestUI(contactPermissionsState)
            } else if (uiState.isLoading && uiState.contacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                ContactList(groupedContacts = uiState.contacts)
            }
        }
    }
}