package com.example.headlesscrm.controller;

import com.example.headlesscrm.domain.InvitationStatus;
import com.example.headlesscrm.domain.OrganizationRole;
import com.example.headlesscrm.dto.*;
import com.example.headlesscrm.exception.GlobalExceptionHandler;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.WorkspaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceControllerTest {

    @Mock
    private WorkspaceService workspaceService;

    @InjectMocks
    private WorkspaceController workspaceController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID workspaceId;
    private UUID memberId;
    private UUID invitationId;
    private OrganizationResponse organizationResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        workspaceId = UUID.randomUUID();
        memberId = UUID.randomUUID();
        invitationId = UUID.randomUUID();

        organizationResponse = OrganizationResponse.builder()
                .id(workspaceId)
                .name("Acme Corp")
                .slug("acme-corp")
                .description("Team workspace")
                .currentUserRole(OrganizationRole.OWNER)
                .memberCount(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        com.example.headlesscrm.domain.User mockUser = new com.example.headlesscrm.domain.User();
        mockUser.setEmail("user@example.com");
        mockUser.setPassword("password");
        mockUser.setRoles(Collections.singletonList("ROLE_USER"));

        UserDetailsImpl userDetails = new UserDetailsImpl(mockUser);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return userDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(workspaceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    void createWorkspace_Returns201Created() throws Exception {
        OrganizationRequest request = OrganizationRequest.builder()
                .name("Acme Corp")
                .build();

        when(workspaceService.createWorkspace(any(OrganizationRequest.class), eq("user@example.com")))
                .thenReturn(organizationResponse);

        mockMvc.perform(post("/api/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(workspaceId.toString()))
                .andExpect(jsonPath("$.name").value("Acme Corp"));
    }

    @Test
    void getUserWorkspaces_Returns200Ok() throws Exception {
        when(workspaceService.getUserWorkspaces("user@example.com"))
                .thenReturn(Collections.singletonList(organizationResponse));

        mockMvc.perform(get("/api/workspaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(workspaceId.toString()));
    }

    @Test
    void getWorkspaceById_Returns200Ok() throws Exception {
        when(workspaceService.getWorkspaceById(workspaceId, "user@example.com"))
                .thenReturn(organizationResponse);

        mockMvc.perform(get("/api/workspaces/{workspaceId}", workspaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workspaceId.toString()));
    }

    @Test
    void updateWorkspace_Returns200Ok() throws Exception {
        OrganizationRequest request = OrganizationRequest.builder()
                .name("Updated Acme")
                .build();

        when(workspaceService.updateWorkspace(eq(workspaceId), any(OrganizationRequest.class), eq("user@example.com")))
                .thenReturn(organizationResponse);

        mockMvc.perform(put("/api/workspaces/{workspaceId}", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workspaceId.toString()));
    }

    @Test
    void deleteWorkspace_Returns204NoContent() throws Exception {
        doNothing().when(workspaceService).deleteWorkspace(workspaceId, "user@example.com");

        mockMvc.perform(delete("/api/workspaces/{workspaceId}", workspaceId))
                .andExpect(status().isNoContent());

        verify(workspaceService).deleteWorkspace(workspaceId, "user@example.com");
    }

    @Test
    void addMember_Returns201Created() throws Exception {
        AddMemberRequest request = AddMemberRequest.builder()
                .email("colleague@example.com")
                .role(OrganizationRole.MEMBER)
                .build();

        OrganizationMemberResponse memberResponse = OrganizationMemberResponse.builder()
                .id(memberId)
                .userEmail("colleague@example.com")
                .role(OrganizationRole.MEMBER)
                .build();

        when(workspaceService.addMember(eq(workspaceId), any(AddMemberRequest.class), eq("user@example.com")))
                .thenReturn(memberResponse);

        mockMvc.perform(post("/api/workspaces/{workspaceId}/members", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(memberId.toString()))
                .andExpect(jsonPath("$.userEmail").value("colleague@example.com"));
    }

    @Test
    void inviteMember_Returns201Created() throws Exception {
        InviteMemberRequest request = InviteMemberRequest.builder()
                .email("invitee@example.com")
                .role(OrganizationRole.MEMBER)
                .build();

        OrganizationInvitationResponse invitationResponse = OrganizationInvitationResponse.builder()
                .id(invitationId)
                .email("invitee@example.com")
                .role(OrganizationRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .build();

        when(workspaceService.inviteMember(eq(workspaceId), any(InviteMemberRequest.class), eq("user@example.com")))
                .thenReturn(invitationResponse);

        mockMvc.perform(post("/api/workspaces/{workspaceId}/invitations", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(invitationId.toString()))
                .andExpect(jsonPath("$.email").value("invitee@example.com"));
    }

    @Test
    void acceptInvitation_Returns200Ok() throws Exception {
        when(workspaceService.acceptInvitation(invitationId, "user@example.com"))
                .thenReturn(organizationResponse);

        mockMvc.perform(post("/api/workspaces/invitations/{invitationId}/accept", invitationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workspaceId.toString()));
    }
}
