package com.plezha.veryspecialcontactsapp.contactservice;

parcelable ContactDetails;

interface IContactService {
    List<ContactDetails> getContacts();
    int deleteDuplicateContacts();
}