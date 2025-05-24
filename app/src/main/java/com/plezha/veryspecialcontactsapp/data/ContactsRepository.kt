package com.plezha.veryspecialcontactsapp.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.plezha.veryspecialcontactsapp.contactservice.ContactDetails
import com.plezha.veryspecialcontactsapp.contactservice.IContactService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOGGING_TAG = "ContactRepositoryImpl"

sealed class DeduplicationResult {
    data class Success(val count: Int) : DeduplicationResult()
    data class PermissionError(val message: String = "Permission denied for deleting contacts.") : DeduplicationResult()
    data class Error(val message: String) : DeduplicationResult()
    object NoDuplicates : DeduplicationResult()
}

interface ContactRepository {
    fun getContacts(): Flow<Result<List<ContactDetails>>>
    suspend fun deleteDuplicateContacts(): DeduplicationResult

    fun connect()
    fun disconnect()
}

class ContactRepositoryImpl(
    private val context: Context,
    private val externalScope: CoroutineScope
) : ContactRepository {

    private var contactService: IContactService? = null
    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(LOGGING_TAG, "Service connected")
            contactService = IContactService.Stub.asInterface(service)
            _isServiceBound.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(LOGGING_TAG, "Service disconnected")
            contactService = null
            _isServiceBound.value = false
        }
    }

    override fun connect() {
        if (contactService == null && !_isServiceBound.value) {
            val intent = Intent("com.example.yourappname.BIND_CONTACT_SERVICE")
            intent.setPackage(context.packageName)
            try {
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                Log.d(LOGGING_TAG, "Attempting to bind service")
            } catch (e: Exception) {
                Log.e(LOGGING_TAG, "Exception binding to service", e)
            }
        }
    }

    override fun disconnect() {
        if (_isServiceBound.value && contactService != null) {
            try {
                context.unbindService(serviceConnection)
                Log.d(LOGGING_TAG, "Service unbound")
            } catch (e: IllegalArgumentException) {
                Log.w(LOGGING_TAG, "Service not registered or already unbound: ${e.message}")
            }
            contactService = null
            _isServiceBound.value = false
        }
    }

    private suspend fun <T> callService(block: suspend (IContactService) -> T): T {
        if (!_isServiceBound.value || contactService == null) {
            if (!_isServiceBound.value) {
                Log.d(LOGGING_TAG, "Service not bound, waiting...")
                _isServiceBound.first { it } // Wait until true
                Log.d(LOGGING_TAG, "Service now bound.")
            }
            if (contactService == null) throw IllegalStateException("Service not connected after wait.")
        }
        return block(contactService!!)
    }


    override fun getContacts(): Flow<Result<List<ContactDetails>>> = callbackFlow {
        externalScope.launch(Dispatchers.IO) {
            try {
                val contacts = callService { it.getContacts() }
                trySend(Result.success(contacts ?: emptyList()))
            } catch (e: Exception) {
                Log.e(LOGGING_TAG, "Exception fetching contacts", e)
                trySend(Result.failure(e))
            }
        }
        awaitClose {
            Log.d(LOGGING_TAG, "getContacts flow closing")
        }
    }


    override suspend fun deleteDuplicateContacts(): DeduplicationResult {
        return withContext(Dispatchers.IO) {
            try {
                val service = contactService
                if (service == null) {
                    return@withContext DeduplicationResult.Error("Service not available.")
                }
                val deletedCount = service.deleteDuplicateContacts()
                when (deletedCount) {
                    -2 -> DeduplicationResult.PermissionError()
                    -1 -> DeduplicationResult.Error("An error occurred in the service during deduplication.")
                    0 -> DeduplicationResult.NoDuplicates
                    else -> DeduplicationResult.Success(deletedCount)
                }
            } catch (e: RemoteException) {
                Log.e(LOGGING_TAG, "RemoteException deleting duplicates", e)
                DeduplicationResult.Error("Remote communication error: ${e.message}")
            } catch (e: Exception) {
                Log.e(LOGGING_TAG, "Exception deleting duplicates", e)
                DeduplicationResult.Error("An unexpected error occurred: ${e.message}")
            }
        }
    }
}