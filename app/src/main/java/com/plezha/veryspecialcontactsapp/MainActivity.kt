package com.plezha.veryspecialcontactsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.plezha.veryspecialcontactsapp.data.ContactRepository
import com.plezha.veryspecialcontactsapp.data.ContactRepositoryImpl
import com.plezha.veryspecialcontactsapp.ui.ContactScreen
import com.plezha.veryspecialcontactsapp.ui.ContactsViewModel
import com.plezha.veryspecialcontactsapp.ui.theme.VerySpecialContactsAppTheme


class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ContactsViewModel
    private lateinit var contactRepository: ContactRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contactRepository = ContactRepositoryImpl(
            applicationContext, lifecycleScope
        )
        viewModel = ContactsViewModel(
            contactRepository = contactRepository
        )

        setContent {
            VerySpecialContactsAppTheme {
                ContactScreen(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        contactRepository.connect()
    }

    override fun onStop() {
        super.onStop()
        contactRepository.disconnect()
    }
}