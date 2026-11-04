package com.example.headlesscrm.service.impl;

import com.example.headlesscrm.domain.Contact;
import com.example.headlesscrm.domain.Customer;
import com.example.headlesscrm.dto.ContactRequest;
import com.example.headlesscrm.dto.ContactResponse;
import com.example.headlesscrm.exception.DuplicateResourceException;
import com.example.headlesscrm.exception.ResourceNotFoundException;
import com.example.headlesscrm.repository.ContactRepository;
import com.example.headlesscrm.service.ContactService;
import com.example.headlesscrm.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final CustomerService customerService;

    @Override
    @Transactional
    public ContactResponse createContact(ContactRequest contactRequest, UUID customerId, String userEmail) {
        Customer customer = customerService.getCustomer(customerId, userEmail);

        if (contactRepository.existsByEmailAndCustomer(contactRequest.getEmail(), customer)) {
            throw new DuplicateResourceException("Contact with email '" + contactRequest.getEmail() + "' already exists for customer id: " + customerId);
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

    @Override
    @Transactional(readOnly = true)
    public Page<ContactResponse> getContactsByCustomer(UUID customerId, String userEmail, Pageable pageable) {
        Customer customer = customerService.getCustomer(customerId, userEmail);
        return contactRepository.findAllByCustomer(customer, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ContactResponse getContactById(UUID customerId, UUID contactId, String userEmail) {
        Customer customer = customerService.getCustomer(customerId, userEmail);
        Contact contact = findContactOrThrow(contactId, customer);
        return mapToResponse(contact);
    }

    @Override
    @Transactional
    public ContactResponse updateContact(UUID customerId, UUID contactId, ContactRequest request, String userEmail) {
        Customer customer = customerService.getCustomer(customerId, userEmail);
        Contact contact = findContactOrThrow(contactId, customer);

        if (!contact.getEmail().equalsIgnoreCase(request.getEmail()) &&
                contactRepository.existsByEmailAndCustomerAndIdNot(request.getEmail(), customer, contactId)) {
            throw new DuplicateResourceException("Contact with email '" + request.getEmail() + "' already exists for customer id: " + customerId);
        }

        contact.setFirstName(request.getFirstName());
        contact.setLastName(request.getLastName());
        contact.setEmail(request.getEmail());
        contact.setPhone(request.getPhone());

        Contact updatedContact = contactRepository.save(contact);
        return mapToResponse(updatedContact);
    }

    @Override
    @Transactional
    public void deleteContact(UUID customerId, UUID contactId, String userEmail) {
        Customer customer = customerService.getCustomer(customerId, userEmail);
        Contact contact = findContactOrThrow(contactId, customer);
        contactRepository.delete(contact);
    }

    private Contact findContactOrThrow(UUID contactId, Customer customer) {
        return contactRepository.findByIdAndCustomer(contactId, customer)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + contactId + " for customer id: " + customer.getId()));
    }

    private ContactResponse mapToResponse(Contact contact) {
        return ContactResponse.builder()
                .id(contact.getId())
                .firstName(contact.getFirstName())
                .lastName(contact.getLastName())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .customerId(contact.getCustomer().getId())
                .build();
    }
}
