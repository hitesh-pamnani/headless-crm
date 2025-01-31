package com.example.headlesscrm.service.impl;

import com.example.headlesscrm.domain.Customer;
import com.example.headlesscrm.domain.User;
import com.example.headlesscrm.dto.CustomerRequest;
import com.example.headlesscrm.dto.CustomerResponse;
import com.example.headlesscrm.repository.CustomerRepository;
import com.example.headlesscrm.repository.UserRepository;
import com.example.headlesscrm.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Override
    public CustomerResponse createCustomer(CustomerRequest customerRequest, String userEmail) {
        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (customerRepository.existsByEmailAndOwner(customerRequest.getEmail(), owner)) {
            throw new RuntimeException("Email already exists");
        }

        Customer customer = Customer.builder()
                .firstName(customerRequest.getFirstName())
                .lastName(customerRequest.getLastName())
                .email(customerRequest.getEmail())
                .phone(customerRequest.getPhone())
                .address(customerRequest.getAddress())
                .owner(owner)
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        return mapToResponse(savedCustomer);
    }

    @Override
    public Page<CustomerResponse> getAllCustomers(String userEmail, Pageable pageable) {
        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return customerRepository.findAllByOwner(owner, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Customer getCustomer(UUID customerId, String userEmail) {
        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return customerRepository.findByIdAndOwner(customerId, owner)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

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
                .build();
    }
}
