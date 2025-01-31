package com.example.headlesscrm.controller;

import com.example.headlesscrm.dto.CustomerRequest;
import com.example.headlesscrm.dto.CustomerResponse;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        CustomerResponse response = customerService.createCustomer(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CustomerResponse> response = customerService.getAllCustomers(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }
}
