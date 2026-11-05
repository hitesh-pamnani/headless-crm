package com.example.headlesscrm.service;

import com.example.headlesscrm.domain.Contact;
import com.example.headlesscrm.domain.Customer;
import com.example.headlesscrm.domain.Organization;
import com.example.headlesscrm.domain.User;
import com.example.headlesscrm.dto.ContactRequest;
import com.example.headlesscrm.dto.ContactResponse;
import com.example.headlesscrm.exception.DuplicateResourceException;
import com.example.headlesscrm.exception.ResourceNotFoundException;
import com.example.headlesscrm.repository.ContactRepository;
import com.example.headlesscrm.security.WorkspaceSecurityService;
import com.example.headlesscrm.service.impl.ContactServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private WorkspaceSecurityService workspaceSecurityService;

    @InjectMocks
    private ContactServiceImpl contactService;

    private Organization organization;
    private Customer customer;
    private Contact contact;
    private ContactRequest contactRequest;
    private UUID customerId;
    private UUID contactId;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        contactId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        organization = Organization.builder()
                .id(organizationId)
                .name("Acme Org")
                .slug("acme-org")
                .build();

        User owner = new User();
        owner.setEmail("user@example.com");

        customer = Customer.builder()
                .id(customerId)
                .firstName("Acme")
                .lastName("Corp")
                .email("info@acme.com")
                .organization(organization)
                .owner(owner)
                .build();

        contact = Contact.builder()
                .id(contactId)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@acme.com")
                .phone("+9876543210")
                .customer(customer)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        contactRequest = new ContactRequest();
        contactRequest.setFirstName("Jane");
        contactRequest.setLastName("Doe");
        contactRequest.setEmail("jane.doe@acme.com");
        contactRequest.setPhone("+9876543210");
    }

    @Test
    void createContact_Success() {
        when(customerService.getCustomer(customerId, "user@example.com")).thenReturn(customer);
        when(contactRepository.existsByEmailAndCustomer("jane.doe@acme.com", customer)).thenReturn(false);
        when(contactRepository.save(any(Contact.class))).thenReturn(contact);

        ContactResponse response = contactService.createContact(contactRequest, customerId, "user@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(contactId);
        assertThat(response.getEmail()).isEqualTo("jane.doe@acme.com");
        assertThat(response.getCustomerId()).isEqualTo(customerId);
        verify(workspaceSecurityService).validateCanModify(organizationId, "user@example.com");
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    void createContact_ReadOnlyMember_ThrowsAccessDeniedException() {
        when(customerService.getCustomer(customerId, "user@example.com")).thenReturn(customer);
        doThrow(new AccessDeniedException("Read-only members cannot modify data"))
                .when(workspaceSecurityService).validateCanModify(organizationId, "user@example.com");

        assertThatThrownBy(() -> contactService.createContact(contactRequest, customerId, "user@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Read-only");
    }

    @Test
    void createContact_CustomerNotFound_ThrowsResourceNotFoundException() {
        when(customerService.getCustomer(customerId, "user@example.com"))
                .thenThrow(new ResourceNotFoundException("Customer not found with id: " + customerId));

        assertThatThrownBy(() -> contactService.createContact(contactRequest, customerId, "user@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void createContact_DuplicateEmail_ThrowsDuplicateResourceException() {
        when(customerService.getCustomer(customerId, "user@example.com")).thenReturn(customer);
        when(contactRepository.existsByEmailAndCustomer("jane.doe@acme.com", customer)).thenReturn(true);

        assertThatThrownBy(() -> contactService.createContact(contactRequest, customerId, "user@example.com"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists for customer id");
    }

    @Test
    void getContactsByCustomer_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(customerService.getCustomer(customerId, "user@example.com")).thenReturn(customer);
        when(contactRepository.findAllByCustomer(customer, pageable))
                .thenReturn(new PageImpl<>(Collections.singletonList(contact)));

        Page<ContactResponse> page = contactService.getContactsByCustomer(customerId, "user@example.com", pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getEmail()).isEqualTo("jane.doe@acme.com");
    }

    @Test
    void getContactById_Success() {
        when(customerService.getCustomer(customerId, "user@example.com")).thenReturn(customer);
        when(contactRepository.findByIdAndCustomer(contactId, customer)).thenReturn(Optional.of(contact));

        ContactResponse response = contactService.getContactById(customerId, contactId, "user@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(contactId);
    }

    @Test
    void getContactById_NotFound_ThrowsResourceNotFoundException() {
        when(customerService.getCustomer(customerId, "user@example.com")).thenReturn(customer);
        when(contactRepository.findByIdAndCustomer(contactId, customer)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.getContactById(customerId, contactId, "user@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Contact not found");
    }

    @Test
    void updateContact_Success_SameEmail() {
        when(customerService.getCustomer(customerId, "user@example.com")).thenReturn(customer);
        when(contactRepository.findByIdAndCustomer(contactId, customer)).thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class))).thenReturn(contact);

        contactRequest.setFirstName("Janet");
        ContactResponse response = contactService.updateContact(customerId, contactId, contactRequest, "user@example.com");

        assertThat(response).isNotNull();
        verify(workspaceSecurityService).validateCanModify(organizationId, "user@example.com");
        verify(contactRepository).save(contact);
        assertThat(contact.getFirstName()).isEqualTo("Janet");
    }

    @Test
    void updateContact_Success_ChangedNonConflictingEmail() {
        when(customerService.getCustomer(customerId, "user@example.com")).thenReturn(customer);
        when(contactRepository.findByIdAndCustomer(contactId, customer)).thenReturn(Optional.of(contact));
        when(contactRepository.existsByEmailAndCustomerAndIdNot("janet.doe@acme.com", customer, contactId)).thenReturn(false);
        when(contactRepository.save(any(Contact.class))).thenReturn(contact);

        contactRequest.setEmail("janet.doe@acme.com");
        ContactResponse response = contactService.updateContact(customerId, contactId, contactRequest, "user@example.com");

        assertThat(response).isNotNull();
        verify(contactRepository).save(contact);
        assertThat(contact.getEmail()).isEqualTo("janet.doe@acme.com");
    }

    @Test
    void updateContact_DuplicateEmailConflict_ThrowsDuplicateResourceException() {
        when(customerService.getCustomer(customerId, "user@example.com")).thenReturn(customer);
        when(contactRepository.findByIdAndCustomer(contactId, customer)).thenReturn(Optional.of(contact));
        when(contactRepository.existsByEmailAndCustomerAndIdNot("taken@acme.com", customer, contactId)).thenReturn(true);

        contactRequest.setEmail("taken@acme.com");

        assertThatThrownBy(() -> contactService.updateContact(customerId, contactId, contactRequest, "user@example.com"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists for customer id");
        verify(contactRepository, never()).save(any());
    }

    @Test
    void deleteContact_Success() {
        when(customerService.getCustomer(customerId, "user@example.com")).thenReturn(customer);
        when(contactRepository.findByIdAndCustomer(contactId, customer)).thenReturn(Optional.of(contact));

        contactService.deleteContact(customerId, contactId, "user@example.com");

        verify(workspaceSecurityService).validateCanDelete(organizationId, "user@example.com");
        verify(contactRepository).delete(contact);
    }
}
