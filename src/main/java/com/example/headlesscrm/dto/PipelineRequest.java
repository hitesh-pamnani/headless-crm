package com.example.headlesscrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pipeline creation or update request")
public class PipelineRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Pipeline name", example = "Sales Pipeline")
    private String name;

    @Size(max = 500)
    @Schema(description = "Pipeline description", example = "Standard inbound sales cycle")
    private String description;

    @Schema(description = "Whether this is the default workspace pipeline", example = "true")
    private Boolean isDefault;

    @Valid
    @Builder.Default
    @Schema(description = "Ordered list of stages for this pipeline")
    private List<PipelineStageRequest> stages = new ArrayList<>();
}
