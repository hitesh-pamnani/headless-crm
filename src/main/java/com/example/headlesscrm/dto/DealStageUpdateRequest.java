package com.example.headlesscrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update deal pipeline stage")
public class DealStageUpdateRequest {

    @NotNull
    @Schema(description = "Target stage UUID")
    private UUID stageId;
}
