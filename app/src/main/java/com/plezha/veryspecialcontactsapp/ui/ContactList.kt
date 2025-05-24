package com.plezha.veryspecialcontactsapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.plezha.veryspecialcontactsapp.contactservice.ContactDetails

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactList(groupedContacts: Map<Char, List<ContactDetails>>) {
    if (groupedContacts.isEmpty()) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No contacts to display.")
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        groupedContacts.forEach { (letter, contactsInGroup) ->
            stickyHeader {
                Text(
                    text = letter.toString(),
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    fontWeight = MaterialTheme.typography.titleMedium.fontWeight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(contactsInGroup, key = { contact -> contact.id }) { contact ->
                ContactRow(contact = contact)
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ContactRow(contact: ContactDetails) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(contact.photoUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .padding(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = contact.displayName ?: "No Name",
                style = MaterialTheme.typography.titleMedium
            )
            contact.phoneNumbers.forEach { phone ->
                Text(
                    text = "Phone number: $phone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            contact.emails.forEach { email ->
                Text(
                    text = "Email: $email",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}