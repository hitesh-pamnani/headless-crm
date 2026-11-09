package com.example.headlesscrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated relationship history event item")
public class TimelineItemResponse {

    @Schema(description = "Unique event identifier", example = "activity-3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String id;

    @Schema(description = "Chronological event timestamp")
    private LocalDateTime timestamp;

    @Schema(description = "Event type (CALL, MEETING, NOTE, TASK, DEAL_CREATED, CONTACT_CREATED, CUSTOMER_CREATED)", example = "MEETING")
    private String type;

    @Schema(description = "Event category (ACTIVITY, MILESTONE, SYSTEM)", example = "ACTIVITY")
    private String category;

    @Schema(description = "Event title or summary", example = "Discovery call held with CTO")
    private String title;

    @Schema(description = "Event description or note content")
    private String description;

    @Schema(description = "Name of the user who logged or triggered this event", example = "Jane Doe")
    private String actorName;

    @Schema(description = "Email of the user who logged or triggered this event", example = "jane@example.com")
    private String actorEmail;

    @Schema(description = "Target entity type (CUSTOMER, CONTACT, DEAL, ACTIVITY)", example = "CUSTOMER")
    private String entityType;

    @Schema(description = "Target entity UUID")
    private UUID entityId;

    @Builder.Default
    @Schema(description = "Structured metadata relevant to the event (e.g. deal value, duration, status)")
    private Map<String, Object> metadata = new HashMap<>();
}
