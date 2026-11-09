package com.example.headlesscrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Contact details response")
public class ContactResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private UUID customerId;

    @Schema(description = "Dynamic custom attributes / JSON metadata")
    private Map<String, Object> customFields;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
