package com.example.headlesscrm.service.impl;

import com.example.headlesscrm.domain.Contact;
import com.example.headlesscrm.domain.Customer;
import com.example.headlesscrm.dto.ContactRequest;
import com.example.headlesscrm.dto.ContactResponse;
import com.example.headlesscrm.repository.ContactRepository;
import com.example.headlesscrm.service.ContactService;
import com.example.headlesscrm.service.CustomerService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final CustomerService customerService;

    @Override
    public ContactResponse createContact(ContactRequest contactRequest, UUID customerId, String userEmail) {
        Customer customer = customerService.getCustomer(customerId, userEmail);

        if (contactRepository.existsByEmailAndCustomer(contactRequest.getEmail(), customer)) {
            throw new RuntimeException("Contact email already exists for this customer");
        }

        Contact contact = Contact.builder()
                .firstName(contactRequest.getFirstName())
                .lastName(contactRequest.getLastName())
                .email(contactRequest.getEmail())
                .phone(contactRequest.getPhone())
                .customer(customer)
                .build();

        Contact savedContact = contactRepository.save(contact);
        return mapToResponse(savedContact);
    }

    private ContactResponse mapToResponse(Contact contact) {
        return ContactResponse.builder()
                .id(contact.getId())
                .firstName(contact.getFirstName())
                .lastName(contact.getLastName())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .createdAt(contact.getCreatedAt())
                .customerId(contact.getCustomer().getId())
                .build();
    }

    @Override
    public Page<ContactResponse> getAllContacts(UUID customerId, String userEmail, Pageable pageable) {
        Customer customer = customerService.getCustomer(customerId, userEmail);
        Page<Contact> contacts = contactRepository.findAllByCustomer(customer, pageable);
        return contacts.map(this::mapToResponse);
    }
}
