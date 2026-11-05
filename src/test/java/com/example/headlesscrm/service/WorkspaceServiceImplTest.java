package com.example.headlesscrm.service;

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
import com.example.headlesscrm.service.impl.WorkspaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceImplTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private OrganizationInvitationRepository organizationInvitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceSecurityService workspaceSecurityService;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    private User user;
    private User otherUser;
    private Organization organization;
    private OrganizationMember ownerMember;
    private OrganizationMember otherMember;
    private UUID workspaceId;
    private UUID userId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setName("Alice");
        user.setEmail("alice@example.com");

        otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setName("Bob");
        otherUser.setEmail("bob@example.com");

        organization = Organization.builder()
                .id(workspaceId)
                .name("Acme Corp")
                .slug("acme-corp")
                .description("HQ CRM workspace")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ownerMember = OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .user(user)
                .role(OrganizationRole.OWNER)
                .joinedAt(LocalDateTime.now())
                .build();

        otherMember = OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .user(otherUser)
                .role(OrganizationRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createWorkspace_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(organizationRepository.existsBySlug("acme-corp")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenReturn(organization);

        OrganizationRequest request = OrganizationRequest.builder()
                .name("Acme Corp")
                .slug("acme-corp")
                .description("HQ CRM workspace")
                .build();

        OrganizationResponse response = workspaceService.createWorkspace(request, "alice@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(workspaceId);
        assertThat(response.getName()).isEqualTo("Acme Corp");
        assertThat(response.getCurrentUserRole()).isEqualTo(OrganizationRole.OWNER);
        verify(organizationMemberRepository).save(any(OrganizationMember.class));
    }

    @Test
    void getUserWorkspaces_ReturnsListOfWorkspaces() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(organizationMemberRepository.findAllByUser(user)).thenReturn(Collections.singletonList(ownerMember));
        when(organizationMemberRepository.countByOrganization(organization)).thenReturn(2L);

        List<OrganizationResponse> workspaces = workspaceService.getUserWorkspaces("alice@example.com");

        assertThat(workspaces).hasSize(1);
        assertThat(workspaces.get(0).getName()).isEqualTo("Acme Corp");
        assertThat(workspaces.get(0).getMemberCount()).isEqualTo(2L);
    }

    @Test
    void getWorkspaceById_Success() {
        when(workspaceSecurityService.getMembershipOrThrow(workspaceId, "alice@example.com")).thenReturn(ownerMember);
        when(organizationMemberRepository.countByOrganization(organization)).thenReturn(1L);

        OrganizationResponse response = workspaceService.getWorkspaceById(workspaceId, "alice@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(workspaceId);
        assertThat(response.getCurrentUserRole()).isEqualTo(OrganizationRole.OWNER);
    }

    @Test
    void addMember_Success() {
        when(organizationRepository.findById(workspaceId)).thenReturn(Optional.of(organization));
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(otherUser));
        when(organizationMemberRepository.existsByOrganizationAndUser(organization, otherUser)).thenReturn(false);
        when(organizationMemberRepository.save(any(OrganizationMember.class))).thenReturn(otherMember);

        AddMemberRequest request = AddMemberRequest.builder()
                .email("bob@example.com")
                .role(OrganizationRole.MEMBER)
                .build();

        OrganizationMemberResponse response = workspaceService.addMember(workspaceId, request, "alice@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getUserEmail()).isEqualTo("bob@example.com");
        assertThat(response.getRole()).isEqualTo(OrganizationRole.MEMBER);
        verify(workspaceSecurityService).validateAdminOrOwner(workspaceId, "alice@example.com");
    }

    @Test
    void addMember_UserAlreadyMember_ThrowsDuplicateResourceException() {
        when(organizationRepository.findById(workspaceId)).thenReturn(Optional.of(organization));
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(otherUser));
        when(organizationMemberRepository.existsByOrganizationAndUser(organization, otherUser)).thenReturn(true);

        AddMemberRequest request = AddMemberRequest.builder()
                .email("bob@example.com")
                .role(OrganizationRole.MEMBER)
                .build();

        assertThatThrownBy(() -> workspaceService.addMember(workspaceId, request, "alice@example.com"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    void inviteMember_Success() {
        when(organizationRepository.findById(workspaceId)).thenReturn(Optional.of(organization));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("newbie@example.com")).thenReturn(Optional.empty());
        when(organizationInvitationRepository.existsByOrganizationAndEmailIgnoreCaseAndStatus(organization, "newbie@example.com", InvitationStatus.PENDING))
                .thenReturn(false);

        OrganizationInvitation invitation = OrganizationInvitation.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .email("newbie@example.com")
                .role(OrganizationRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .invitedBy(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(organizationInvitationRepository.save(any(OrganizationInvitation.class))).thenReturn(invitation);

        InviteMemberRequest request = InviteMemberRequest.builder()
                .email("newbie@example.com")
                .role(OrganizationRole.MEMBER)
                .build();

        OrganizationInvitationResponse response = workspaceService.inviteMember(workspaceId, request, "alice@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("newbie@example.com");
        assertThat(response.getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void acceptInvitation_Success() {
        UUID invitationId = UUID.randomUUID();
        OrganizationInvitation invitation = OrganizationInvitation.builder()
                .id(invitationId)
                .organization(organization)
                .email("bob@example.com")
                .role(OrganizationRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .invitedBy(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(organizationInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(otherUser));
        when(organizationMemberRepository.existsByOrganizationAndUser(organization, otherUser)).thenReturn(false);
        when(organizationMemberRepository.countByOrganization(organization)).thenReturn(2L);

        OrganizationResponse response = workspaceService.acceptInvitation(invitationId, "bob@example.com");

        assertThat(response).isNotNull();
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        verify(organizationMemberRepository).save(any(OrganizationMember.class));
    }

    @Test
    void removeMember_SoleOwnerLeaving_ThrowsBadRequestException() {
        when(organizationRepository.findById(workspaceId)).thenReturn(Optional.of(organization));
        when(workspaceSecurityService.getMembershipOrThrow(workspaceId, "alice@example.com")).thenReturn(ownerMember);
        when(organizationMemberRepository.findById(ownerMember.getId())).thenReturn(Optional.of(ownerMember));
        when(organizationMemberRepository.findAllByOrganization(organization)).thenReturn(Collections.singletonList(ownerMember));

        assertThatThrownBy(() -> workspaceService.removeMember(workspaceId, ownerMember.getId(), "alice@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("sole owner cannot leave");
    }

    @Test
    void resolveWorkspace_FallbackToDefaultWorkspace() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(organizationMemberRepository.findFirstByUserOrderByJoinedAtAsc(user)).thenReturn(Optional.of(ownerMember));

        Organization resolved = workspaceService.resolveWorkspace(null, "alice@example.com");

        assertThat(resolved).isNotNull();
        assertThat(resolved.getId()).isEqualTo(workspaceId);
    }
}
