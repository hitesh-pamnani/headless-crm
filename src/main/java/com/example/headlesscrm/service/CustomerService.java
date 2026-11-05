package com.example.headlesscrm.service;

import com.example.headlesscrm.domain.Customer;
import com.example.headlesscrm.dto.CustomerRequest;
import com.example.headlesscrm.dto.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {

    CustomerResponse createCustomer(UUID workspaceId, CustomerRequest customerRequest, String userEmail);

    default CustomerResponse createCustomer(CustomerRequest customerRequest, String userEmail) {
        return createCustomer(null, customerRequest, userEmail);
    }

    Page<CustomerResponse> getAllCustomers(UUID workspaceId, String userEmail, Pageable pageable);

    default Page<CustomerResponse> getAllCustomers(String userEmail, Pageable pageable) {
        return getAllCustomers(null, userEmail, pageable);
    }

    Customer getCustomer(UUID customerId, String userEmail);

    CustomerResponse getCustomerById(UUID customerId, String userEmail);

    CustomerResponse updateCustomer(UUID customerId, CustomerRequest request, String userEmail);

    void deleteCustomer(UUID customerId, String userEmail);
}
