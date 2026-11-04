package com.example.headlesscrm.service;

import com.example.headlesscrm.dto.ContactRequest;
import com.example.headlesscrm.dto.ContactResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ContactService {
    ContactResponse createContact(ContactRequest contactRequest, UUID customerId, String userEmail);

    Page<ContactResponse> getContactsByCustomer(UUID customerId, String userEmail, Pageable pageable);

    default Page<ContactResponse> getAllContacts(UUID customerId, String userEmail, Pageable pageable) {
        return getContactsByCustomer(customerId, userEmail, pageable);
    }

    ContactResponse getContactById(UUID customerId, UUID contactId, String userEmail);

    ContactResponse updateContact(UUID customerId, UUID contactId, ContactRequest request, String userEmail);

    void deleteContact(UUID customerId, UUID contactId, String userEmail);
}
