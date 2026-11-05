package com.example.headlesscrm.service;

import com.example.headlesscrm.domain.Organization;
import com.example.headlesscrm.domain.User;
import com.example.headlesscrm.dto.*;

import java.util.List;
import java.util.UUID;

public interface WorkspaceService {

    OrganizationResponse createWorkspace(OrganizationRequest request, String userEmail);

    List<OrganizationResponse> getUserWorkspaces(String userEmail);

    OrganizationResponse getWorkspaceById(UUID workspaceId, String userEmail);

    OrganizationResponse updateWorkspace(UUID workspaceId, OrganizationRequest request, String userEmail);

    void deleteWorkspace(UUID workspaceId, String userEmail);

    List<OrganizationMemberResponse> getWorkspaceMembers(UUID workspaceId, String userEmail);

    OrganizationMemberResponse addMember(UUID workspaceId, AddMemberRequest request, String userEmail);

    OrganizationMemberResponse updateMemberRole(UUID workspaceId, UUID memberId, UpdateMemberRoleRequest request, String userEmail);

    void removeMember(UUID workspaceId, UUID memberId, String userEmail);

    OrganizationInvitationResponse inviteMember(UUID workspaceId, InviteMemberRequest request, String userEmail);

    List<OrganizationInvitationResponse> getPendingInvitations(UUID workspaceId, String userEmail);

    OrganizationResponse acceptInvitation(UUID invitationId, String userEmail);

    void declineInvitation(UUID invitationId, String userEmail);

    Organization resolveWorkspace(UUID workspaceId, String userEmail);

    Organization createDefaultWorkspaceForUser(User user);
}
