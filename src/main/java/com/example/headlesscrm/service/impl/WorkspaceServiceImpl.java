package com.example.headlesscrm.service.impl;

import com.example.headlesscrm.domain.*;
import com.example.headlesscrm.dto.*;
import com.example.headlesscrm.exception.BadRequestException;
import com.example.headlesscrm.exception.DuplicateResourceException;
import com.example.headlesscrm.exception.ResourceNotFoundException;
import com.example.headlesscrm.repository.OrganizationInvitationRepository;
import com.example.headlesscrm.repository.OrganizationMemberRepository;
import com.example.headlesscrm.repository.OrganizationRepository;
import com.example.headlesscrm.repository.UserRepository;
import com.example.headlesscrm.security.WorkspaceSecurityService;
import com.example.headlesscrm.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationInvitationRepository organizationInvitationRepository;
    private final UserRepository userRepository;
    private final WorkspaceSecurityService workspaceSecurityService;

    @Override
    @Transactional
    public OrganizationResponse createWorkspace(OrganizationRequest request, String userEmail) {
        User user = findUserByEmail(userEmail);

        String slug = generateUniqueSlug(request.getName(), request.getSlug(), null);

        Organization organization = Organization.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .build();

        Organization savedOrganization = organizationRepository.save(organization);

        OrganizationMember ownerMember = OrganizationMember.builder()
                .organization(savedOrganization)
                .user(user)
                .role(OrganizationRole.OWNER)
                .build();

        organizationMemberRepository.save(ownerMember);

        return mapToResponse(savedOrganization, OrganizationRole.OWNER, 1);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationResponse> getUserWorkspaces(String userEmail) {
        User user = findUserByEmail(userEmail);
        List<OrganizationMember> memberships = organizationMemberRepository.findAllByUser(user);

        return memberships.stream()
                .map(membership -> {
                    Organization org = membership.getOrganization();
                    long count = organizationMemberRepository.countByOrganization(org);
                    return mapToResponse(org, membership.getRole(), count);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getWorkspaceById(UUID workspaceId, String userEmail) {
        OrganizationMember membership = workspaceSecurityService.getMembershipOrThrow(workspaceId, userEmail);
        Organization organization = membership.getOrganization();
        long count = organizationMemberRepository.countByOrganization(organization);
        return mapToResponse(organization, membership.getRole(), count);
    }

    @Override
    @Transactional
    public OrganizationResponse updateWorkspace(UUID workspaceId, OrganizationRequest request, String userEmail) {
        workspaceSecurityService.validateAdminOrOwner(workspaceId, userEmail);
        OrganizationMember currentMember = workspaceSecurityService.getMembershipOrThrow(workspaceId, userEmail);
        Organization organization = currentMember.getOrganization();

        String slug = generateUniqueSlug(request.getName(), request.getSlug(), workspaceId);

        organization.setName(request.getName().trim());
        organization.setSlug(slug);
        organization.setDescription(request.getDescription());

        Organization updated = organizationRepository.save(organization);
        long count = organizationMemberRepository.countByOrganization(updated);
        return mapToResponse(updated, currentMember.getRole(), count);
    }

    @Override
    @Transactional
    public void deleteWorkspace(UUID workspaceId, String userEmail) {
        workspaceSecurityService.validateOwner(workspaceId, userEmail);
        Organization organization = findOrganizationOrThrow(workspaceId);
        organizationRepository.delete(organization);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationMemberResponse> getWorkspaceMembers(UUID workspaceId, String userEmail) {
        workspaceSecurityService.getMembershipOrThrow(workspaceId, userEmail);
        Organization organization = findOrganizationOrThrow(workspaceId);
        return organizationMemberRepository.findAllByOrganization(organization).stream()
                .map(this::mapToMemberResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrganizationMemberResponse addMember(UUID workspaceId, AddMemberRequest request, String userEmail) {
        workspaceSecurityService.validateAdminOrOwner(workspaceId, userEmail);
        Organization organization = findOrganizationOrThrow(workspaceId);
        User userToAdd = userRepository.findByEmail(request.getEmail().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (organizationMemberRepository.existsByOrganizationAndUser(organization, userToAdd)) {
            throw new DuplicateResourceException("User is already a member of this workspace");
        }

        OrganizationMember member = OrganizationMember.builder()
                .organization(organization)
                .user(userToAdd)
                .role(request.getRole())
                .build();

        OrganizationMember saved = organizationMemberRepository.save(member);
        return mapToMemberResponse(saved);
    }

    @Override
    @Transactional
    public OrganizationMemberResponse updateMemberRole(UUID workspaceId, UUID memberId, UpdateMemberRoleRequest request, String userEmail) {
        workspaceSecurityService.validateAdminOrOwner(workspaceId, userEmail);
        Organization organization = findOrganizationOrThrow(workspaceId);
        OrganizationMember currentMember = workspaceSecurityService.getMembershipOrThrow(workspaceId, userEmail);

        OrganizationMember targetMember = organizationMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found with id: " + memberId));

        if (!targetMember.getOrganization().getId().equals(organization.getId())) {
            throw new BadRequestException("Member does not belong to the specified workspace");
        }

        if (targetMember.getRole() == OrganizationRole.OWNER && currentMember.getRole() != OrganizationRole.OWNER) {
            throw new AccessDeniedException("Only an Owner can modify another Owner's role");
        }

        targetMember.setRole(request.getRole());
        OrganizationMember updated = organizationMemberRepository.save(targetMember);
        return mapToMemberResponse(updated);
    }

    @Override
    @Transactional
    public void removeMember(UUID workspaceId, UUID memberId, String userEmail) {
        Organization organization = findOrganizationOrThrow(workspaceId);
        OrganizationMember currentMember = workspaceSecurityService.getMembershipOrThrow(workspaceId, userEmail);

        OrganizationMember targetMember = organizationMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found with id: " + memberId));

        if (!targetMember.getOrganization().getId().equals(organization.getId())) {
            throw new BadRequestException("Member does not belong to the specified workspace");
        }

        boolean isSelfRemoval = targetMember.getUser().getEmail().equalsIgnoreCase(userEmail);

        if (!isSelfRemoval) {
            workspaceSecurityService.validateAdminOrOwner(workspaceId, userEmail);
            if (targetMember.getRole() == OrganizationRole.OWNER && currentMember.getRole() != OrganizationRole.OWNER) {
                throw new AccessDeniedException("Admins cannot remove a workspace Owner.");
            }
        } else {
            if (targetMember.getRole() == OrganizationRole.OWNER) {
                long ownerCount = organizationMemberRepository.findAllByOrganization(organization).stream()
                        .filter(m -> m.getRole() == OrganizationRole.OWNER)
                        .count();
                if (ownerCount <= 1) {
                    throw new BadRequestException("The sole owner cannot leave the workspace. Transfer ownership or delete the workspace.");
                }
            }
        }

        organizationMemberRepository.delete(targetMember);
    }

    @Override
    @Transactional
    public OrganizationInvitationResponse inviteMember(UUID workspaceId, InviteMemberRequest request, String userEmail) {
        workspaceSecurityService.validateAdminOrOwner(workspaceId, userEmail);
        Organization organization = findOrganizationOrThrow(workspaceId);
        User inviter = findUserByEmail(userEmail);
        String targetEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmail(targetEmail).ifPresent(existingUser -> {
            if (organizationMemberRepository.existsByOrganizationAndUser(organization, existingUser)) {
                throw new DuplicateResourceException("User is already a member of this workspace");
            }
        });

        if (organizationInvitationRepository.existsByOrganizationAndEmailIgnoreCaseAndStatus(organization, targetEmail, InvitationStatus.PENDING)) {
            throw new DuplicateResourceException("An invitation is already pending for " + targetEmail);
        }

        OrganizationInvitation invitation = OrganizationInvitation.builder()
                .organization(organization)
                .email(targetEmail)
                .role(request.getRole())
                .invitedBy(inviter)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        OrganizationInvitation saved = organizationInvitationRepository.save(invitation);
        return mapToInvitationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationInvitationResponse> getPendingInvitations(UUID workspaceId, String userEmail) {
        workspaceSecurityService.validateAdminOrOwner(workspaceId, userEmail);
        Organization organization = findOrganizationOrThrow(workspaceId);
        return organizationInvitationRepository.findAllByOrganizationAndStatus(organization, InvitationStatus.PENDING).stream()
                .map(this::mapToInvitationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrganizationResponse acceptInvitation(UUID invitationId, String userEmail) {
        OrganizationInvitation invitation = organizationInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found with id: " + invitationId));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Invitation is no longer pending");
        }

        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invitation has expired");
        }

        if (!invitation.getEmail().equalsIgnoreCase(userEmail.trim())) {
            throw new AccessDeniedException("This invitation was addressed to " + invitation.getEmail());
        }

        User user = findUserByEmail(userEmail);
        Organization organization = invitation.getOrganization();

        if (!organizationMemberRepository.existsByOrganizationAndUser(organization, user)) {
            OrganizationMember member = OrganizationMember.builder()
                    .organization(organization)
                    .user(user)
                    .role(invitation.getRole())
                    .build();
            organizationMemberRepository.save(member);
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        organizationInvitationRepository.save(invitation);

        long count = organizationMemberRepository.countByOrganization(organization);
        return mapToResponse(organization, invitation.getRole(), count);
    }

    @Override
    @Transactional
    public void declineInvitation(UUID invitationId, String userEmail) {
        OrganizationInvitation invitation = organizationInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found with id: " + invitationId));

        if (!invitation.getEmail().equalsIgnoreCase(userEmail.trim())) {
            throw new AccessDeniedException("This invitation was addressed to " + invitation.getEmail());
        }

        invitation.setStatus(InvitationStatus.REVOKED);
        organizationInvitationRepository.save(invitation);
    }

    @Override
    @Transactional
    public Organization resolveWorkspace(UUID workspaceId, String userEmail) {
        if (workspaceId != null) {
            Organization organization = findOrganizationOrThrow(workspaceId);
            if (!workspaceSecurityService.isMember(workspaceId, userEmail)) {
                throw new AccessDeniedException("You do not have access to workspace id: " + workspaceId);
            }
            return organization;
        }

        User user = findUserByEmail(userEmail);
        return organizationMemberRepository.findFirstByUserOrderByJoinedAtAsc(user)
                .map(OrganizationMember::getOrganization)
                .orElseGet(() -> createDefaultWorkspaceForUser(user));
    }

    @Override
    @Transactional
    public Organization createDefaultWorkspaceForUser(User user) {
        String baseName = user.getName() != null && !user.getName().isBlank()
                ? user.getName() + "'s Workspace"
                : "Default Workspace";
        String slug = generateUniqueSlug(baseName, null, null);

        Organization organization = Organization.builder()
                .name(baseName)
                .slug(slug)
                .description("Default personal CRM workspace")
                .build();

        Organization saved = organizationRepository.save(organization);

        OrganizationMember member = OrganizationMember.builder()
                .organization(saved)
                .user(user)
                .role(OrganizationRole.OWNER)
                .build();

        organizationMemberRepository.save(member);
        return saved;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private Organization findOrganizationOrThrow(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + organizationId));
    }

    private String generateUniqueSlug(String name, String preferredSlug, UUID existingOrgId) {
        String base = (preferredSlug != null && !preferredSlug.isBlank())
                ? preferredSlug.trim().toLowerCase(Locale.ROOT)
                : name.trim().toLowerCase(Locale.ROOT);

        String cleaned = base.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (cleaned.isBlank()) {
            cleaned = "workspace";
        }

        String candidate = cleaned;
        int counter = 1;
        while (slugExists(candidate, existingOrgId)) {
            candidate = cleaned + "-" + counter++;
        }
        return candidate;
    }

    private boolean slugExists(String slug, UUID existingOrgId) {
        if (existingOrgId == null) {
            return organizationRepository.existsBySlug(slug);
        }
        return organizationRepository.existsBySlugAndIdNot(slug, existingOrgId);
    }

    private OrganizationResponse mapToResponse(Organization org, OrganizationRole role, long memberCount) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .slug(org.getSlug())
                .description(org.getDescription())
                .currentUserRole(role)
                .memberCount(memberCount)
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }

    private OrganizationMemberResponse mapToMemberResponse(OrganizationMember member) {
        return OrganizationMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .userName(member.getUser().getName())
                .userEmail(member.getUser().getEmail())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    private OrganizationInvitationResponse mapToInvitationResponse(OrganizationInvitation invitation) {
        return OrganizationInvitationResponse.builder()
                .id(invitation.getId())
                .organizationId(invitation.getOrganization().getId())
                .organizationName(invitation.getOrganization().getName())
                .email(invitation.getEmail())
                .role(invitation.getRole())
                .status(invitation.getStatus())
                .invitedByEmail(invitation.getInvitedBy().getEmail())
                .createdAt(invitation.getCreatedAt())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }
}
