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
@Schema(description = "Response representing a member of a workspace")
public class OrganizationMemberResponse {

    @Schema(description = "Membership identifier")
    private UUID id;

    @Schema(description = "User identifier")
    private UUID userId;

    @Schema(description = "User full name", example = "Jane Doe")
    private String userName;

    @Schema(description = "User email address", example = "jane.doe@example.com")
    private String userEmail;

    @Schema(description = "Role in this workspace", example = "ADMIN")
    private OrganizationRole role;

    @Schema(description = "Date and time the user joined the workspace")
    private LocalDateTime joinedAt;
}
