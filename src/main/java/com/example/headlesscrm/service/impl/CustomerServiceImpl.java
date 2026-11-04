package com.example.headlesscrm.service.impl;

import com.example.headlesscrm.domain.Customer;
import com.example.headlesscrm.domain.User;
import com.example.headlesscrm.dto.CustomerRequest;
import com.example.headlesscrm.dto.CustomerResponse;
import com.example.headlesscrm.exception.DuplicateResourceException;
import com.example.headlesscrm.exception.ResourceNotFoundException;
import com.example.headlesscrm.repository.CustomerRepository;
import com.example.headlesscrm.repository.UserRepository;
import com.example.headlesscrm.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest customerRequest, String userEmail) {
        User owner = findOwnerByEmail(userEmail);

        if (customerRepository.existsByEmailAndOwnerAndDeletedFalse(customerRequest.getEmail(), owner)) {
            throw new DuplicateResourceException("Customer with email '" + customerRequest.getEmail() + "' already exists");
        }

        Customer customer = Customer.builder()
                .firstName(customerRequest.getFirstName())
                .lastName(customerRequest.getLastName())
                .email(customerRequest.getEmail())
                .phone(customerRequest.getPhone())
                .address(customerRequest.getAddress())
                .deleted(false)
                .owner(owner)
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        return mapToResponse(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAllCustomers(String userEmail, Pageable pageable) {
        User owner = findOwnerByEmail(userEmail);
        return customerRepository.findAllByOwnerAndDeletedFalse(owner, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomer(UUID customerId, String userEmail) {
        User owner = findOwnerByEmail(userEmail);
        return customerRepository.findByIdAndOwnerAndDeletedFalse(customerId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
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

        if (!customer.getEmail().equalsIgnoreCase(request.getEmail()) &&
                customerRepository.existsByEmailAndOwnerAndDeletedFalseAndIdNot(request.getEmail(), customer.getOwner(), customerId)) {
            throw new DuplicateResourceException("Customer with email '" + request.getEmail() + "' already exists");
        }

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());

        Customer updatedCustomer = customerRepository.save(customer);
        return mapToResponse(updatedCustomer);
    }

    @Override
    @Transactional
    public void deleteCustomer(UUID customerId, String userEmail) {
        Customer customer = getCustomer(customerId, userEmail);
        customer.setDeleted(true);
        customer.setDeletedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }

    private User findOwnerByEmail(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
