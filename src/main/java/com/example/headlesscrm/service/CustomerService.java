package com.example.headlesscrm.service;

import com.example.headlesscrm.domain.Customer;
import com.example.headlesscrm.dto.CustomerRequest;
import com.example.headlesscrm.dto.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {
    CustomerResponse createCustomer(CustomerRequest customerRequest, String userEmail);
    Page<CustomerResponse> getAllCustomers(String userEmail, Pageable pageable);
    Customer getCustomer(UUID customerId, String userEmail);
}
