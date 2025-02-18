package com.example.headlesscrm.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.headlesscrm.dto.LeadRequest;
import com.example.headlesscrm.dto.LeadResponse;

public interface LeadService {
    LeadResponse createLead(LeadRequest leadRequest, String userEmail);

    Page<LeadResponse> getAllLeads(String userEmail, Pageable pageable);
}
