package com.example.headlesscrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Customer details response")
public class CustomerResponse {
    @Schema(description = "Unique customer identifier")
    private UUID id;

    @Schema(description = "Customer first name", example = "John")
    private String firstName;

    @Schema(description = "Customer last name", example = "Doe")
    private String lastName;

    @Schema(description = "Customer email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Customer phone number", example = "+1234567890")
    private String phone;

    @Schema(description = "Customer address", example = "123 Main Street")
    private String address;

    @Schema(description = "Organization/Workspace identifier the customer belongs to")
    private UUID organizationId;

    @Schema(description = "Organization/Workspace name the customer belongs to", example = "Acme Corp")
    private String organizationName;

    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Record last updated timestamp")
    private LocalDateTime updatedAt;
}
