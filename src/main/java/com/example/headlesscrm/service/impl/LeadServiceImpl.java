package com.example.headlesscrm.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.headlesscrm.domain.Lead;
import com.example.headlesscrm.domain.Lead.LeadStatus;
import com.example.headlesscrm.domain.User;
import com.example.headlesscrm.dto.LeadRequest;
import com.example.headlesscrm.dto.LeadResponse;
import com.example.headlesscrm.repository.LeadRepository;
import com.example.headlesscrm.repository.UserRepository;
import com.example.headlesscrm.service.LeadService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;

    @Override
    public LeadResponse createLead(LeadRequest leadRequest, String userEmail) {
        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (leadRepository.existsByEmailAndOwner(leadRequest.getEmail(), owner)) {
            throw new RuntimeException("Email already exists");
        }

        Lead lead = Lead.builder()
                .firstName(leadRequest.getFirstName())
                .lastName(leadRequest.getLastName())
                .email(leadRequest.getEmail())
                .phone(leadRequest.getPhone())
                .company(leadRequest.getCompany())
                .status(LeadStatus.NEW)
                .owner(owner)
                .build();

        leadRepository.save(lead);

        return mapToResponse(lead);
    }

    private LeadResponse mapToResponse(Lead lead) {
        return LeadResponse.builder()
                .id(lead.getId())
                .firstName(lead.getFirstName())
                .lastName(lead.getLastName())
                .email(lead.getEmail())
                .phone(lead.getPhone())
                .company(lead.getCompany())
                .status(lead.getStatus())
                .createdAt(lead.getCreatedAt())
                .updatedAt(lead.getUpdatedAt())
                .build();
    }

    @Override
    public Page<LeadResponse> getAllLeads(String userEmail, Pageable pageable) {
        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Page<Lead> leads = leadRepository.findAllByOwner(owner, pageable);
        return leads.map(this::mapToResponse);
    }
}
