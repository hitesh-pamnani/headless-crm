package com.example.headlesscrm.service;

import com.example.headlesscrm.domain.Customer;
import com.example.headlesscrm.dto.ContactRequest;
import com.example.headlesscrm.dto.ContactResponse;

import java.util.UUID;

public interface ContactService {
    ContactResponse createContact(ContactRequest contactRequest, UUID customerId, String userEmail);
}
