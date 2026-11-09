package com.example.headlesscrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pipeline response with configured stages")
public class PipelineResponse {
    private UUID id;
    private String name;
    private String description;
    private boolean isDefault;
    private UUID organizationId;

    @Builder.Default
    private List<PipelineStageResponse> stages = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
