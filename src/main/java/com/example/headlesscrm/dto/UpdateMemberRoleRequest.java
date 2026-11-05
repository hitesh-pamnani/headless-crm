package com.example.headlesscrm.dto;

import com.example.headlesscrm.domain.OrganizationRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload to update a workspace member's role")
public class UpdateMemberRoleRequest {

    @NotNull(message = "Role is required")
    @Schema(description = "New role for the member", example = "ADMIN")
    private OrganizationRole role;
}
