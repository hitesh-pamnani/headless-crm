package com.example.headlesscrm.dto;

import com.example.headlesscrm.domain.OrganizationRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload to directly add an existing user to a workspace")
public class AddMemberRequest {

    @NotBlank(message = "User email is required")
    @Email(message = "Must be a valid email address")
    @Schema(description = "Email of the existing user to add", example = "colleague@example.com")
    private String email;

    @NotNull(message = "Role is required")
    @Schema(description = "Role to assign to the new member", example = "MEMBER")
    private OrganizationRole role;
}
