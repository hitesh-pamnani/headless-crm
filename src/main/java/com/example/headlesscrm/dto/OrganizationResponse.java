package com.example.headlesscrm.dto;

import com.example.headlesscrm.domain.OrganizationRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload representing a workspace/organization")
public class OrganizationResponse {

    @Schema(description = "Unique workspace identifier")
    private UUID id;

    @Schema(description = "Workspace name", example = "Acme Corp")
    private String name;

    @Schema(description = "Workspace slug", example = "acme-corp")
    private String slug;

    @Schema(description = "Workspace description", example = "Enterprise CRM workspace for sales team")
    private String description;

    @Schema(description = "Role of current authenticated user in this workspace", example = "OWNER")
    private OrganizationRole currentUserRole;

    @Schema(description = "Total number of members in the workspace", example = "5")
    private long memberCount;

    @Schema(description = "Workspace creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Workspace last updated timestamp")
    private LocalDateTime updatedAt;
}
