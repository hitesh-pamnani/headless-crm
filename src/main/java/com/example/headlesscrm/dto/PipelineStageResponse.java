package com.example.headlesscrm.dto;

import com.example.headlesscrm.domain.StageCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pipeline stage response")
public class PipelineStageResponse {
    private UUID id;
    private String name;
    private int displayOrder;
    private int probability;
    private StageCategory category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
