package com.example.headlesscrm.controller;

import com.example.headlesscrm.dto.CustomerRequest;
import com.example.headlesscrm.dto.CustomerResponse;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer lifecycle management endpoints with multi-tenancy workspace scoping")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Create a new customer", description = "Creates a customer in the specified workspace (or user's default workspace if header omitted).")
    public ResponseEntity<CustomerResponse> createCustomer(
            @Parameter(description = "Optional workspace UUID header. Defaults to user's primary workspace.")
            @RequestHeader(value = "X-Workspace-Id", required = false) UUID workspaceId,
            @Valid @RequestBody CustomerRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        CustomerResponse response = customerService.createCustomer(workspaceId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all active customers", description = "Retrieves a paginated list of non-deleted customers in the workspace.")
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(
            @Parameter(description = "Optional workspace UUID header. Defaults to user's primary workspace.")
            @RequestHeader(value = "X-Workspace-Id", required = false) UUID workspaceId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CustomerResponse> response = customerService.getAllCustomers(workspaceId, userDetails.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Retrieves the details of a specific active customer by ID.")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        CustomerResponse response = customerService.getCustomerById(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer details", description = "Updates an existing active customer in the workspace. Requires MEMBER, ADMIN, or OWNER role.")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        CustomerResponse response = customerService.updateCustomer(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete customer", description = "Marks a customer as deleted. Requires ADMIN or OWNER role.")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        customerService.deleteCustomer(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
