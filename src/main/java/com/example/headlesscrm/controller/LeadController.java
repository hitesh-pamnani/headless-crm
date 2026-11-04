package com.example.headlesscrm.controller;

import com.example.headlesscrm.dto.LeadRequest;
import com.example.headlesscrm.dto.LeadResponse;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.LeadService;
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

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
@Tag(name = "Leads", description = "Lead management endpoints")
public class LeadController {
    private final LeadService leadService;

    @PostMapping
    @Operation(summary = "Create lead", description = "Creates a new lead associated with the authenticated user.")
    public ResponseEntity<LeadResponse> createLead(@Valid @RequestBody LeadRequest leadRequest,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leadService.createLead(leadRequest, userDetails.getUsername()));
    }

    @GetMapping
    @Operation(summary = "List leads", description = "Retrieves a paginated list of leads owned by the authenticated user.")
    public ResponseEntity<Page<LeadResponse>> getAllLeads(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(leadService.getAllLeads(userDetails.getUsername(), pageable));
    }
}
