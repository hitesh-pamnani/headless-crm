package com.example.headlesscrm.dto;

import com.example.headlesscrm.domain.ActivityStatus;
import com.example.headlesscrm.domain.ActivityType;
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
@Schema(description = "Activity interaction response")
public class ActivityResponse {
    private UUID id;
    private ActivityType type;
    private String title;
    private String description;
    private ActivityStatus status;
    private LocalDateTime dueDate;
    private LocalDateTime completedAt;
    private Integer durationMinutes;

    private UUID organizationId;

    private UUID customerId;
    private String customerName;

    private UUID contactId;
    private String contactName;

    private UUID dealId;
    private String dealTitle;

    private UUID userId;
    private String userName;
    private String userEmail;

    @Schema(description = "Dynamic custom attributes / JSON metadata")
    private Map<String, Object> customFields;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
