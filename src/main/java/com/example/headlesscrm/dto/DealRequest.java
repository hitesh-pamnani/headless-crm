package com.example.headlesscrm.dto;

import com.example.headlesscrm.domain.DealStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Deal creation or update request")
public class DealRequest {

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Deal title / Opportunity name", example = "Acme Enterprise License")
    private String title;

    @DecimalMin(value = "0.0", inclusive = true)
    @Schema(description = "Deal monetary value", example = "50000.00")
    private BigDecimal value;

    @Size(max = 3)
    @Schema(description = "Currency code (ISO 4217)", example = "USD")
    private String currency;

    @Schema(description = "Target close date", example = "2026-12-31")
    private LocalDate expectedCloseDate;

    @Schema(description = "Associated pipeline ID. Defaults to workspace default pipeline if omitted.")
    private UUID pipelineId;

    @Schema(description = "Associated stage ID. Defaults to first stage in pipeline if omitted.")
    private UUID stageId;

    @Schema(description = "Associated Customer ID")
    private UUID customerId;

    @Schema(description = "Associated primary Contact ID")
    private UUID contactId;

    @Schema(description = "Deal status (OPEN, WON, LOST)", example = "OPEN")
    private DealStatus status;

    @Schema(description = "Dynamic custom attributes / JSON metadata")
    private Map<String, Object> customFields;
}
