package com.example.headlesscrm.security;

import com.example.headlesscrm.domain.OrganizationMember;
import com.example.headlesscrm.domain.OrganizationRole;
import com.example.headlesscrm.repository.OrganizationMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Component("workspaceSecurity")
@RequiredArgsConstructor
public class WorkspaceSecurityService {

    private final OrganizationMemberRepository organizationMemberRepository;

    public boolean isMember(UUID workspaceId, String userEmail) {
        if (workspaceId == null || userEmail == null) {
            return false;
        }
        return organizationMemberRepository.findByOrganizationIdAndUserEmail(workspaceId, userEmail).isPresent();
    }

    public boolean hasRole(UUID workspaceId, String userEmail, String... roleNames) {
        if (workspaceId == null || userEmail == null) {
            return false;
        }
        return organizationMemberRepository.findByOrganizationIdAndUserEmail(workspaceId, userEmail)
                .map(member -> Arrays.stream(roleNames)
                        .anyMatch(r -> member.getRole().name().equalsIgnoreCase(r)))
                .orElse(false);
    }

    public OrganizationMember getMembershipOrThrow(UUID workspaceId, String userEmail) {
        return organizationMemberRepository.findByOrganizationIdAndUserEmail(workspaceId, userEmail)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this workspace."));
    }

    public void validateCanModify(UUID workspaceId, String userEmail) {
        OrganizationMember member = getMembershipOrThrow(workspaceId, userEmail);
        if (member.getRole() == OrganizationRole.READ_ONLY) {
            throw new AccessDeniedException("Read-only members are not permitted to modify workspace data.");
        }
    }

    public void validateCanDelete(UUID workspaceId, String userEmail) {
        OrganizationMember member = getMembershipOrThrow(workspaceId, userEmail);
        if (member.getRole() != OrganizationRole.OWNER && member.getRole() != OrganizationRole.ADMIN) {
            throw new AccessDeniedException("Only workspace Admins or Owners are permitted to perform delete actions.");
        }
    }

    public void validateAdminOrOwner(UUID workspaceId, String userEmail) {
        OrganizationMember member = getMembershipOrThrow(workspaceId, userEmail);
        if (member.getRole() != OrganizationRole.OWNER && member.getRole() != OrganizationRole.ADMIN) {
            throw new AccessDeniedException("Administrative privileges required for this workspace action.");
        }
    }

    public void validateOwner(UUID workspaceId, String userEmail) {
        OrganizationMember member = getMembershipOrThrow(workspaceId, userEmail);
        if (member.getRole() != OrganizationRole.OWNER) {
            throw new AccessDeniedException("Only the workspace Owner is permitted to perform this action.");
        }
    }
}
