package com.example.headlesscrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for creating or updating a workspace/organization")
public class OrganizationRequest {

    @NotBlank(message = "Workspace name is required")
    @Size(max = 100, message = "Workspace name cannot exceed 100 characters")
    @Schema(description = "Workspace or company name", example = "Acme Corp")
    private String name;

    @Size(max = 100, message = "Slug cannot exceed 100 characters")
    @Schema(description = "Unique workspace URL slug (auto-generated if omitted)", example = "acme-corp")
    private String slug;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Schema(description = "Short description of the workspace", example = "Enterprise CRM workspace for sales team")
    private String description;
}
