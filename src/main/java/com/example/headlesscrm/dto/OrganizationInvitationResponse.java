package com.example.headlesscrm.dto;

import com.example.headlesscrm.domain.InvitationStatus;
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
@Schema(description = "Response payload representing an organization/workspace invitation")
public class OrganizationInvitationResponse {

    @Schema(description = "Unique invitation identifier")
    private UUID id;

    @Schema(description = "Workspace identifier")
    private UUID organizationId;

    @Schema(description = "Workspace name", example = "Acme Corp")
    private String organizationName;

    @Schema(description = "Invited user's email", example = "invitee@example.com")
    private String email;

    @Schema(description = "Role offered in this invitation", example = "MEMBER")
    private OrganizationRole role;

    @Schema(description = "Status of the invitation", example = "PENDING")
    private InvitationStatus status;

    @Schema(description = "Email of the member who issued the invitation", example = "admin@example.com")
    private String invitedByEmail;

    @Schema(description = "Timestamp when the invitation was created")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the invitation expires")
    private LocalDateTime expiresAt;
}
