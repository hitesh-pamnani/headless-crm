package com.example.headlesscrm.dto;

import com.example.headlesscrm.domain.DealStatus;
import com.example.headlesscrm.domain.StageCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Deal opportunity response")
public class DealResponse {
    private UUID id;
    private String title;
    private BigDecimal value;
    private String currency;
    private DealStatus status;
    private LocalDate expectedCloseDate;
    private LocalDateTime actualCloseDate;

    private UUID pipelineId;
    private String pipelineName;

    private UUID stageId;
    private String stageName;
    private int stageProbability;
    private StageCategory stageCategory;

    private UUID organizationId;
    private String organizationName;

    private UUID customerId;
    private String customerName;
    private String customerEmail;

    private UUID contactId;
    private String contactName;

    private UUID ownerId;
    private String ownerName;
    private String ownerEmail;

    @Schema(description = "Dynamic custom attributes / JSON metadata")
    private Map<String, Object> customFields;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
