package com.plezha.veryspecialcontactsapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestUI(permissionsState: MultiplePermissionsState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "This app needs access to your contacts to display them and manage duplicates. " +
                    "Please grant Read and Write Contacts permissions."
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
            Text("Grant Permissions")
        }
        if (permissionsState.shouldShowRationale) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Permissions are important for this app to function.", color = MaterialTheme.colorScheme.error)
        }
    }
}