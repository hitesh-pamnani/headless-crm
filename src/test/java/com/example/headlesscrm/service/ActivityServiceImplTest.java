package com.example.headlesscrm.service;

import com.example.headlesscrm.domain.*;
import com.example.headlesscrm.dto.ActivityRequest;
import com.example.headlesscrm.dto.ActivityResponse;
import com.example.headlesscrm.repository.*;
import com.example.headlesscrm.security.WorkspaceSecurityService;
import com.example.headlesscrm.service.impl.ActivityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceImplTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private DealRepository dealRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private WorkspaceSecurityService workspaceSecurityService;

    @InjectMocks
    private ActivityServiceImpl activityService;

    private Organization organization;
    private User user;
    private Customer customer;
    private Activity activity;
    private UUID workspaceId;
    private UUID activityId;
    private final String userEmail = "rep@example.com";

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        activityId = UUID.randomUUID();

        organization = Organization.builder()
                .id(workspaceId)
                .name("Acme Corp")
                .slug("acme-corp")
                .build();

        user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Sales Rep");
        user.setEmail(userEmail);

        customer = Customer.builder()
                .id(UUID.randomUUID())
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .organization(organization)
                .build();

        activity = Activity.builder()
                .id(activityId)
                .type(ActivityType.CALL)
                .title("Follow-up Call")
                .description("Discuss pricing tiers")
                .status(ActivityStatus.PENDING)
                .dueDate(LocalDateTime.now().plusDays(2))
                .organization(organization)
                .customer(customer)
                .user(user)
                .customFields(new HashMap<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createActivity_Meeting_Success() {
        ActivityRequest request = ActivityRequest.builder()
                .type(ActivityType.MEETING)
                .title("Product Demo")
                .description("Showcase API capabilities")
                .dueDate(LocalDateTime.now().plusDays(1))
                .durationMinutes(45)
                .customerId(customer.getId())
                .build();

        when(workspaceService.resolveWorkspace(workspaceId, userEmail)).thenReturn(organization);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(customerRepository.findByIdAndOrganizationAndDeletedFalse(customer.getId(), organization)).thenReturn(Optional.of(customer));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> {
            Activity a = inv.getArgument(0);
            a.setId(activityId);
            return a;
        });

        ActivityResponse response = activityService.createActivity(workspaceId, request, userEmail);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Product Demo");
        assertThat(response.getType()).isEqualTo(ActivityType.MEETING);
        assertThat(response.getStatus()).isEqualTo(ActivityStatus.PENDING);
        assertThat(response.getDurationMinutes()).isEqualTo(45);
        verify(workspaceSecurityService).validateCanModify(workspaceId, userEmail);
    }

    @Test
    void createActivity_Note_DefaultsToCompleted() {
        ActivityRequest request = ActivityRequest.builder()
                .type(ActivityType.NOTE)
                .title("Call summary note")
                .description("Client requested SOC2 compliance doc")
                .build();

        when(workspaceService.resolveWorkspace(workspaceId, userEmail)).thenReturn(organization);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> {
            Activity a = inv.getArgument(0);
            a.setId(activityId);
            return a;
        });

        ActivityResponse response = activityService.createActivity(workspaceId, request, userEmail);

        assertThat(response.getType()).isEqualTo(ActivityType.NOTE);
        assertThat(response.getStatus()).isEqualTo(ActivityStatus.COMPLETED);
        assertThat(response.getCompletedAt()).isNotNull();
    }

    @Test
    void createActivity_ReadOnlyMember_ThrowsAccessDenied() {
        ActivityRequest request = ActivityRequest.builder()
                .type(ActivityType.CALL)
                .title("Call")
                .build();

        when(workspaceService.resolveWorkspace(workspaceId, userEmail)).thenReturn(organization);
        doThrow(new AccessDeniedException("Read-only members are not permitted to modify workspace data."))
                .when(workspaceSecurityService).validateCanModify(workspaceId, userEmail);

        assertThatThrownBy(() -> activityService.createActivity(workspaceId, request, userEmail))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getActivities_Filtered_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Activity> activityPage = new PageImpl<>(Collections.singletonList(activity), pageable, 1);

        when(workspaceService.resolveWorkspace(workspaceId, userEmail)).thenReturn(organization);
        when(activityRepository.findFiltered(organization, null, null, null, null, null, pageable))
                .thenReturn(activityPage);

        Page<ActivityResponse> result = activityService.getActivities(workspaceId, null, null, null, null, null, userEmail, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Follow-up Call");
        verify(workspaceSecurityService).getMembershipOrThrow(workspaceId, userEmail);
    }

    @Test
    void getActivityById_Success() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

        ActivityResponse response = activityService.getActivityById(activityId, userEmail);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(activityId);
        verify(workspaceSecurityService).getMembershipOrThrow(workspaceId, userEmail);
    }

    @Test
    void completeActivity_SetsCompletedAndTimestamp() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityResponse response = activityService.completeActivity(activityId, userEmail);

        assertThat(response.getStatus()).isEqualTo(ActivityStatus.COMPLETED);
        assertThat(response.getCompletedAt()).isNotNull();
        verify(workspaceSecurityService).validateCanModify(workspaceId, userEmail);
    }

    @Test
    void updateActivity_Success() {
        ActivityRequest request = ActivityRequest.builder()
                .type(ActivityType.CALL)
                .title("Rescheduled Call")
                .description("Updated notes")
                .build();

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityResponse response = activityService.updateActivity(activityId, request, userEmail);

        assertThat(response.getTitle()).isEqualTo("Rescheduled Call");
        assertThat(response.getDescription()).isEqualTo("Updated notes");
        verify(workspaceSecurityService).validateCanModify(workspaceId, userEmail);
    }

    @Test
    void deleteActivity_Success() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

        activityService.deleteActivity(activityId, userEmail);

        verify(activityRepository).delete(activity);
        verify(workspaceSecurityService).validateCanDelete(workspaceId, userEmail);
    }
}
