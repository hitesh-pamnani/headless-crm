package com.example.headlesscrm.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.headlesscrm.dto.LeadRequest;
import com.example.headlesscrm.dto.LeadResponse;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.LeadService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {
    private final LeadService leadService;

    @PostMapping
    public ResponseEntity<LeadResponse> createLead(@Valid @RequestBody LeadRequest leadRequest,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leadService.createLead(leadRequest, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<Page<LeadResponse>> getAllLeads(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(leadService.getAllLeads(userDetails.getUsername(), pageable));
    }
}
