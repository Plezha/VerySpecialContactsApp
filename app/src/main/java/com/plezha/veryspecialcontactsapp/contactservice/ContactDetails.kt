package com.plezha.veryspecialcontactsapp.contactservice

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContactDetails(
    val id: Long,
    val lookupKey: String,
    val displayName: String?,
    val phoneNumbers: List<String>,
    val emails: List<String>,
    val photoUri: String?
) : Parcelable {
    val signature: String
        get() =
            "$displayName ${
                phoneNumbers.sorted().joinToString("_")
            } ${
                emails.sorted().joinToString("_")
            } $photoUri"
}
