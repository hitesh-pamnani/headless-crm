package com.example.headlesscrm.service;

import com.example.headlesscrm.dto.ContactRequest;
import com.example.headlesscrm.dto.ContactResponse;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContactService {
    ContactResponse createContact(ContactRequest contactRequest, UUID customerId, String userEmail);

    Page<ContactResponse> getAllContacts(UUID customerId, String userEmail, Pageable pageable);
}
