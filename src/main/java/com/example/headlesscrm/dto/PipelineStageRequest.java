package com.example.headlesscrm.dto;

import com.example.headlesscrm.domain.StageCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pipeline stage request")
public class PipelineStageRequest {

    @Schema(description = "Optional ID if updating an existing stage")
    private UUID id;

    @NotBlank
    @Schema(description = "Stage name", example = "Qualified")
    private String name;

    @Schema(description = "Display order for the stage in the pipeline", example = "1")
    private int displayOrder;

    @Min(0)
    @Max(100)
    @Schema(description = "Win probability percentage (0 - 100)", example = "30")
    private int probability;

    @Schema(description = "Stage category (OPEN, WON, LOST)", example = "OPEN")
    private StageCategory category;
}
