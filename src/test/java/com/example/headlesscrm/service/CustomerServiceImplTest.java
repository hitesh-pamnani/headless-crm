package com.example.headlesscrm.service;

import com.example.headlesscrm.domain.Customer;
import com.example.headlesscrm.domain.Organization;
import com.example.headlesscrm.domain.User;
import com.example.headlesscrm.dto.CustomerRequest;
import com.example.headlesscrm.dto.CustomerResponse;
import com.example.headlesscrm.exception.DuplicateResourceException;
import com.example.headlesscrm.exception.ResourceNotFoundException;
import com.example.headlesscrm.repository.CustomerRepository;
import com.example.headlesscrm.repository.UserRepository;
import com.example.headlesscrm.security.WorkspaceSecurityService;
import com.example.headlesscrm.service.impl.CustomerServiceImpl;
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
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private WorkspaceSecurityService workspaceSecurityService;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private User owner;
    private Organization organization;
    private Customer customer;
    private CustomerRequest customerRequest;
    private UUID customerId;
    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();

        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("user@example.com");

        organization = Organization.builder()
                .id(workspaceId)
                .name("Acme Corp")
                .slug("acme-corp")
                .build();

        customer = Customer.builder()
                .id(customerId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .address("123 Main St")
                .organization(organization)
                .owner(owner)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        customerRequest = new CustomerRequest();
        customerRequest.setFirstName("John");
        customerRequest.setLastName("Doe");
        customerRequest.setEmail("john.doe@example.com");
        customerRequest.setPhone("+1234567890");
        customerRequest.setAddress("123 Main St");
    }

    @Test
    void createCustomer_Success() {
        when(workspaceService.resolveWorkspace(null, "user@example.com")).thenReturn(organization);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(owner));
        when(customerRepository.existsByEmailAndOrganizationAndDeletedFalse("john.doe@example.com", organization)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerResponse response = customerService.createCustomer(customerRequest, "user@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(customerId);
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
        verify(workspaceSecurityService).validateCanModify(organization.getId(), "user@example.com");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void createCustomer_OwnerNotFound_ThrowsResourceNotFoundException() {
        when(workspaceService.resolveWorkspace(null, "user@example.com")).thenReturn(organization);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.createCustomer(customerRequest, "user@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void createCustomer_DuplicateEmailInWorkspace_ThrowsDuplicateResourceException() {
        when(workspaceService.resolveWorkspace(null, "user@example.com")).thenReturn(organization);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(owner));
        when(customerRepository.existsByEmailAndOrganizationAndDeletedFalse("john.doe@example.com", organization)).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(customerRequest, "user@example.com"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createCustomer_ReadOnlyRole_ThrowsAccessDeniedException() {
        when(workspaceService.resolveWorkspace(null, "user@example.com")).thenReturn(organization);
        doThrow(new AccessDeniedException("Read-only members are not permitted to modify workspace data."))
                .when(workspaceSecurityService).validateCanModify(organization.getId(), "user@example.com");

        assertThatThrownBy(() -> customerService.createCustomer(customerRequest, "user@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Read-only");
    }

    @Test
    void getAllCustomers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(workspaceService.resolveWorkspace(null, "user@example.com")).thenReturn(organization);
        when(customerRepository.findAllByOrganizationAndDeletedFalse(organization, pageable))
                .thenReturn(new PageImpl<>(Collections.singletonList(customer)));

        Page<CustomerResponse> page = customerService.getAllCustomers("user@example.com", pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void getCustomerById_Success() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.getCustomerById(customerId, "user@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(customerId);
        verify(workspaceSecurityService).getMembershipOrThrow(organization.getId(), "user@example.com");
    }

    @Test
    void getCustomerById_NotFound_ThrowsResourceNotFoundException() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(customerId, "user@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void updateCustomer_Success_SameEmail() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        customerRequest.setFirstName("Johnny");
        CustomerResponse response = customerService.updateCustomer(customerId, customerRequest, "user@example.com");

        assertThat(response).isNotNull();
        verify(workspaceSecurityService).validateCanModify(organization.getId(), "user@example.com");
        verify(customerRepository).save(customer);
        assertThat(customer.getFirstName()).isEqualTo("Johnny");
    }

    @Test
    void updateCustomer_Success_ChangedNonConflictingEmail() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailAndOrganizationAndDeletedFalseAndIdNot("new.email@example.com", organization, customerId)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        customerRequest.setEmail("new.email@example.com");
        CustomerResponse response = customerService.updateCustomer(customerId, customerRequest, "user@example.com");

        assertThat(response).isNotNull();
        verify(customerRepository).save(customer);
        assertThat(customer.getEmail()).isEqualTo("new.email@example.com");
    }

    @Test
    void updateCustomer_DuplicateEmailConflict_ThrowsDuplicateResourceException() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailAndOrganizationAndDeletedFalseAndIdNot("taken@example.com", organization, customerId)).thenReturn(true);

        customerRequest.setEmail("taken@example.com");

        assertThatThrownBy(() -> customerService.updateCustomer(customerId, customerRequest, "user@example.com"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
        verify(customerRepository, never()).save(any());
    }

    @Test
    void deleteCustomer_Success() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        customerService.deleteCustomer(customerId, "user@example.com");

        assertThat(customer.isDeleted()).isTrue();
        assertThat(customer.getDeletedAt()).isNotNull();
        verify(workspaceSecurityService).validateCanDelete(organization.getId(), "user@example.com");
        verify(customerRepository).save(customer);
    }
}
