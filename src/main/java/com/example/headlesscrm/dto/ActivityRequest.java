package com.example.headlesscrm.dto;

import com.example.headlesscrm.domain.ActivityStatus;
import com.example.headlesscrm.domain.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Activity logging or task creation request")
public class ActivityRequest {

    @NotNull
    @Schema(description = "Activity type (CALL, MEETING, NOTE, TASK)", example = "MEETING")
    private ActivityType type;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Activity title or subject", example = "Quarterly Business Review")
    private String title;

    @Schema(description = "Detailed activity description, notes, or meeting summary", example = "Reviewed ARR growth and next quarter milestones.")
    private String description;

    @Schema(description = "Activity status (PENDING, COMPLETED, CANCELLED)", example = "COMPLETED")
    private ActivityStatus status;

    @Schema(description = "Due date or scheduled date/time for the activity", example = "2026-11-15T14:00:00")
    private LocalDateTime dueDate;

    @Schema(description = "Duration in minutes (e.g. for calls and meetings)", example = "45")
    private Integer durationMinutes;

    @Schema(description = "Optional associated Customer UUID")
    private UUID customerId;

    @Schema(description = "Optional associated Contact UUID")
    private UUID contactId;

    @Schema(description = "Optional associated Deal UUID")
    private UUID dealId;

    @Schema(description = "Dynamic custom attributes / JSON metadata")
    private Map<String, Object> customFields;
}
