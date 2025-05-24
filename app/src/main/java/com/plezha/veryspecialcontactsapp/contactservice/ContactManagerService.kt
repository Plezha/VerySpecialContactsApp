package com.plezha.veryspecialcontactsapp.contactservice


import android.Manifest
import android.app.Service
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.IBinder
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.SupervisorJob

private const val LOGGING_TAG = "ContactManagerService"

class ContactManagerService : Service() {
    private val serviceJob = SupervisorJob()

    private val binder = object : IContactService.Stub() {
        override fun getContacts(): List<ContactDetails>? {
            if (!hasReadContactsPermission()) {
                return mutableListOf()
            }

            return fetchContacts()
        }

        override fun deleteDuplicateContacts(): Int {
            if (!hasReadContactsPermission() || !hasWriteContactsPermission()) {
                return -2
            }
            return deduplicate()
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun hasReadContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasWriteContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun fetchContacts(): MutableList<ContactDetails> {


        val contactsList = mutableListOf<ContactDetails>()
        val contentResolver: ContentResolver = applicationContext.contentResolver

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_URI
        )

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC"
        )

        cursor?.use { c ->
            val idCol = c.getColumnIndex(ContactsContract.Contacts._ID)
            val lookupKeyCol = c.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
            val nameCol = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoUriCol = c.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val lookupKey = c.getString(lookupKeyCol)
                val name = c.getString(nameCol)
                val photoUriString = c.getString(photoUriCol)

                val phoneNumbers = getPhoneNumbers(contentResolver, id)
                val emails = getEmails(contentResolver, id)

                contactsList.add(
                    ContactDetails(
                        id,
                        lookupKey,
                        name,
                        phoneNumbers,
                        emails,
                        photoUriString
                    )
                )
            }
        }
        Log.d(LOGGING_TAG, "Fetched ${contactsList.size} contacts")

        return contactsList
    }

    private fun getPhoneNumbers(resolver: ContentResolver, contactId: Long): List<String> {
        val numbers = mutableListOf<String>()
        val phoneCursor: Cursor? = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
            arrayOf(contactId.toString()),
            null
        )
        phoneCursor?.use { pc ->
            val numberCol = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (pc.moveToNext()) {
                numbers.add(pc.getString(numberCol))
            }
        }
        return numbers
    }

    private fun getEmails(resolver: ContentResolver, contactId: Long): List<String> {
        val emails = mutableListOf<String>()
        val emailCursor: Cursor? = resolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
            arrayOf(contactId.toString()),
            null
        )
        emailCursor?.use { ec ->
            val emailCol = ec.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
            while (ec.moveToNext()) {
                emails.add(ec.getString(emailCol))
            }
        }
        return emails
    }

    private fun deduplicate(): Int {
        val allContacts = fetchContacts()
        if (allContacts.isEmpty()) return 0

        val uniqueSignatures = mutableMapOf<String, ContactDetails>()
        val duplicatesToDelete = mutableListOf<ContactDetails>()

        for (contact in allContacts) {
            val signature = contact.signature
            if (uniqueSignatures.containsKey(signature)) {
                duplicatesToDelete.add(contact)
            } else {
                uniqueSignatures[signature] = contact
            }
        }

        if (duplicatesToDelete.isEmpty()) {
            Log.d(LOGGING_TAG, "No duplicates found.")
            return 0
        }

        Log.d(LOGGING_TAG, "Found ${duplicatesToDelete.size} duplicates to delete.")
        val ops = ArrayList<ContentProviderOperation>()
        duplicatesToDelete.forEach { duplicate ->
            val contactUri =
                ContactsContract.Contacts.getLookupUri(duplicate.id, duplicate.lookupKey)
            ops.add(ContentProviderOperation.newDelete(contactUri).build())
        }

        return try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Log.d(LOGGING_TAG, "Successfully deleted ${duplicatesToDelete.size} duplicates.")
            duplicatesToDelete.size
        } catch (e: SecurityException) {
            Log.e(LOGGING_TAG, "Security exception during deletion: ", e)
            -2
        } catch (e: Exception) {
            Log.e(LOGGING_TAG, "Generic error deleting duplicates: ", e)
            -1
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}