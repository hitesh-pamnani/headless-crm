package com.example.headlesscrm.controller;

import com.example.headlesscrm.dto.ContactRequest;
import com.example.headlesscrm.dto.ContactResponse;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers/{customerId}/contacts")
@RequiredArgsConstructor
public class ContactController {
    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<ContactResponse> createContact(
            @PathVariable UUID customerId,
            @Valid @RequestBody ContactRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ContactResponse response = contactService.createContact(request, customerId, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
