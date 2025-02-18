package com.example.headlesscrm.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.headlesscrm.domain.Lead.LeadStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeadResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String company;
    private LeadStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
