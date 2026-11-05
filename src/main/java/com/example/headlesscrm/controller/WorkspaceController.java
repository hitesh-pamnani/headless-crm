package com.example.headlesscrm.controller;

import com.example.headlesscrm.dto.*;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
@Tag(name = "Workspaces", description = "Multi-tenancy workspace, team membership, and invitation management endpoints")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    @Operation(summary = "Create workspace", description = "Creates a new workspace/organization. The authenticated user becomes the OWNER.")
    public ResponseEntity<OrganizationResponse> createWorkspace(
            @Valid @RequestBody OrganizationRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        OrganizationResponse response = workspaceService.createWorkspace(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List workspaces", description = "Retrieves all workspaces the authenticated user belongs to.")
    public ResponseEntity<List<OrganizationResponse>> getUserWorkspaces(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(workspaceService.getUserWorkspaces(userDetails.getUsername()));
    }

    @GetMapping("/{workspaceId}")
    @Operation(summary = "Get workspace by ID", description = "Retrieves details of a specific workspace.")
    public ResponseEntity<OrganizationResponse> getWorkspaceById(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(workspaceService.getWorkspaceById(workspaceId, userDetails.getUsername()));
    }

    @PutMapping("/{workspaceId}")
    @Operation(summary = "Update workspace", description = "Updates workspace name, slug, or description. Requires ADMIN or OWNER role.")
    public ResponseEntity<OrganizationResponse> updateWorkspace(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody OrganizationRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(workspaceService.updateWorkspace(workspaceId, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{workspaceId}")
    @Operation(summary = "Delete workspace", description = "Deletes a workspace and its data. Requires OWNER role.")
    public ResponseEntity<Void> deleteWorkspace(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        workspaceService.deleteWorkspace(workspaceId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{workspaceId}/members")
    @Operation(summary = "List workspace members", description = "Retrieves all members of a workspace and their roles.")
    public ResponseEntity<List<OrganizationMemberResponse>> getWorkspaceMembers(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(workspaceService.getWorkspaceMembers(workspaceId, userDetails.getUsername()));
    }

    @PostMapping("/{workspaceId}/members")
    @Operation(summary = "Add member directly", description = "Adds an existing registered user to the workspace. Requires ADMIN or OWNER role.")
    public ResponseEntity<OrganizationMemberResponse> addMember(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        OrganizationMemberResponse response = workspaceService.addMember(workspaceId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{workspaceId}/members/{memberId}")
    @Operation(summary = "Update member role", description = "Changes a workspace member's role. Requires ADMIN or OWNER role.")
    public ResponseEntity<OrganizationMemberResponse> updateMemberRole(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(workspaceService.updateMemberRole(workspaceId, memberId, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{workspaceId}/members/{memberId}")
    @Operation(summary = "Remove member or leave workspace", description = "Removes a member from the workspace, or leaves the workspace.")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        workspaceService.removeMember(workspaceId, memberId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workspaceId}/invitations")
    @Operation(summary = "Invite member", description = "Sends a workspace invitation to an email address. Requires ADMIN or OWNER role.")
    public ResponseEntity<OrganizationInvitationResponse> inviteMember(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody InviteMemberRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        OrganizationInvitationResponse response = workspaceService.inviteMember(workspaceId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{workspaceId}/invitations")
    @Operation(summary = "List pending invitations", description = "Retrieves all pending invitations for a workspace. Requires ADMIN or OWNER role.")
    public ResponseEntity<List<OrganizationInvitationResponse>> getPendingInvitations(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(workspaceService.getPendingInvitations(workspaceId, userDetails.getUsername()));
    }

    @PostMapping("/invitations/{invitationId}/accept")
    @Operation(summary = "Accept invitation", description = "Accepts a workspace invitation and joins the workspace.")
    public ResponseEntity<OrganizationResponse> acceptInvitation(
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(workspaceService.acceptInvitation(invitationId, userDetails.getUsername()));
    }

    @PostMapping("/invitations/{invitationId}/decline")
    @Operation(summary = "Decline invitation", description = "Declines and revokes a pending workspace invitation.")
    public ResponseEntity<Void> declineInvitation(
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        workspaceService.declineInvitation(invitationId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
