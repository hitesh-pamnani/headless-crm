package com.example.headlesscrm.controller;

import com.example.headlesscrm.dto.ContactRequest;
import com.example.headlesscrm.dto.ContactResponse;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/customers/{customerId}/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts", description = "Customer contact management endpoints")
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    @Operation(summary = "Create contact for customer", description = "Creates a new contact associated with a specific customer.")
    public ResponseEntity<ContactResponse> createContact(
            @PathVariable UUID customerId,
            @Valid @RequestBody ContactRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ContactResponse response = contactService.createContact(request, customerId, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List contacts for customer", description = "Retrieves a paginated list of contacts belonging to the specified customer.")
    public ResponseEntity<Page<ContactResponse>> getContactsByCustomer(
            @PathVariable UUID customerId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ContactResponse> response = contactService.getContactsByCustomer(customerId, userDetails.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{contactId}")
    @Operation(summary = "Get contact by ID", description = "Retrieves contact details by customer and contact IDs.")
    public ResponseEntity<ContactResponse> getContactById(
            @PathVariable UUID customerId,
            @PathVariable UUID contactId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ContactResponse response = contactService.getContactById(customerId, contactId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{contactId}")
    @Operation(summary = "Update contact details", description = "Updates an existing contact associated with a specific customer.")
    public ResponseEntity<ContactResponse> updateContact(
            @PathVariable UUID customerId,
            @PathVariable UUID contactId,
            @Valid @RequestBody ContactRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ContactResponse response = contactService.updateContact(customerId, contactId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{contactId}")
    @Operation(summary = "Delete contact", description = "Deletes a contact from the specified customer.")
    public ResponseEntity<Void> deleteContact(
            @PathVariable UUID customerId,
            @PathVariable UUID contactId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        contactService.deleteContact(customerId, contactId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
