package com.example.headlesscrm.service.impl;

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
import com.example.headlesscrm.service.CustomerService;
import com.example.headlesscrm.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;
    private final WorkspaceSecurityService workspaceSecurityService;

    @Override
    @Transactional
    public CustomerResponse createCustomer(UUID workspaceId, CustomerRequest customerRequest, String userEmail) {
        Organization organization = workspaceService.resolveWorkspace(workspaceId, userEmail);
        workspaceSecurityService.validateCanModify(organization.getId(), userEmail);
        User owner = findOwnerByEmail(userEmail);

        if (customerRepository.existsByEmailAndOrganizationAndDeletedFalse(customerRequest.getEmail(), organization)) {
            throw new DuplicateResourceException("Customer with email '" + customerRequest.getEmail() + "' already exists in this workspace");
        }

        Customer customer = Customer.builder()
                .firstName(customerRequest.getFirstName())
                .lastName(customerRequest.getLastName())
                .email(customerRequest.getEmail())
                .phone(customerRequest.getPhone())
                .address(customerRequest.getAddress())
                .customFields(customerRequest.getCustomFields() != null ? customerRequest.getCustomFields() : new HashMap<>())
                .deleted(false)
                .organization(organization)
                .owner(owner)
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        return mapToResponse(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAllCustomers(UUID workspaceId, String userEmail, Pageable pageable) {
        Organization organization = workspaceService.resolveWorkspace(workspaceId, userEmail);
        return customerRepository.findAllByOrganizationAndDeletedFalse(organization, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomer(UUID customerId, String userEmail) {
        Customer customer = customerRepository.findById(customerId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        workspaceSecurityService.getMembershipOrThrow(customer.getOrganization().getId(), userEmail);
        return customer;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID customerId, String userEmail) {
        Customer customer = getCustomer(customerId, userEmail);
        return mapToResponse(customer);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(UUID customerId, CustomerRequest request, String userEmail) {
        Customer customer = getCustomer(customerId, userEmail);
        workspaceSecurityService.validateCanModify(customer.getOrganization().getId(), userEmail);

        if (!customer.getEmail().equalsIgnoreCase(request.getEmail()) &&
                customerRepository.existsByEmailAndOrganizationAndDeletedFalseAndIdNot(request.getEmail(), customer.getOrganization(), customerId)) {
            throw new DuplicateResourceException("Customer with email '" + request.getEmail() + "' already exists in this workspace");
        }

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        if (request.getCustomFields() != null) {
            customer.setCustomFields(request.getCustomFields());
        }

        Customer updatedCustomer = customerRepository.save(customer);
        return mapToResponse(updatedCustomer);
    }

    @Override
    @Transactional
    public void deleteCustomer(UUID customerId, String userEmail) {
        Customer customer = getCustomer(customerId, userEmail);
        workspaceSecurityService.validateCanDelete(customer.getOrganization().getId(), userEmail);

        customer.setDeleted(true);
        customer.setDeletedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }

    private User findOwnerByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .customFields(customer.getCustomFields())
                .organizationId(customer.getOrganization() != null ? customer.getOrganization().getId() : null)
                .organizationName(customer.getOrganization() != null ? customer.getOrganization().getName() : null)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
