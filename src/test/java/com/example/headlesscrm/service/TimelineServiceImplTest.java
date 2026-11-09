package com.example.headlesscrm.service;

import com.example.headlesscrm.domain.*;
import com.example.headlesscrm.dto.TimelineItemResponse;
import com.example.headlesscrm.repository.*;
import com.example.headlesscrm.security.WorkspaceSecurityService;
import com.example.headlesscrm.service.impl.TimelineServiceImpl;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimelineServiceImplTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private DealRepository dealRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private WorkspaceSecurityService workspaceSecurityService;

    @InjectMocks
    private TimelineServiceImpl timelineService;

    private Organization organization;
    private Customer customer;
    private Contact contact;
    private Deal deal;
    private Activity activity;
    private UUID customerId;
    private UUID dealId;
    private UUID workspaceId;
    private final String userEmail = "admin@example.com";

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        dealId = UUID.randomUUID();

        organization = Organization.builder()
                .id(workspaceId)
                .name("Acme Corp")
                .slug("acme-corp")
                .build();

        customer = Customer.builder()
                .id(customerId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .organization(organization)
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();

        contact = Contact.builder()
                .id(UUID.randomUUID())
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .customer(customer)
                .createdAt(LocalDateTime.now().minusDays(8))
                .build();

        deal = Deal.builder()
                .id(dealId)
                .title("Renewal Contract")
                .value(new BigDecimal("25000.00"))
                .currency("USD")
                .status(DealStatus.WON)
                .customer(customer)
                .organization(organization)
                .createdAt(LocalDateTime.now().minusDays(5))
                .actualCloseDate(LocalDateTime.now().minusDays(2))
                .build();

        activity = Activity.builder()
                .id(UUID.randomUUID())
                .type(ActivityType.CALL)
                .title("Kickoff Call")
                .description("Introductory conversation")
                .status(ActivityStatus.COMPLETED)
                .customer(customer)
                .organization(organization)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    void getCustomerTimeline_AggregatesCustomerMilestonesAndActivitiesChronologically() {
        Pageable pageable = PageRequest.of(0, 10);

        when(customerService.getCustomer(customerId, userEmail)).thenReturn(customer);
        when(contactRepository.findAllByCustomer(eq(customer), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(contact)));
        when(dealRepository.findAllByCustomerAndDeletedFalseOrderByCreatedAtDesc(customer))
                .thenReturn(Collections.singletonList(deal));
        when(activityRepository.findAllByCustomerOrderByCreatedAtDesc(customer))
                .thenReturn(Collections.singletonList(activity));

        Page<TimelineItemResponse> timeline = timelineService.getCustomerTimeline(customerId, userEmail, pageable);

        assertThat(timeline).isNotNull();
        // 1 customer creation + 1 contact + 1 deal created + 1 deal closed (won) + 1 activity = 5 items
        assertThat(timeline.getContent()).hasSize(5);

        // Verify chronological descending order: newest first
        List<TimelineItemResponse> content = timeline.getContent();
        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).getTimestamp())
                    .isAfterOrEqualTo(content.get(i + 1).getTimestamp());
        }

        // Newest should be activity (minus 1 day)
        assertThat(content.get(0).getType()).isEqualTo("CALL");
        // Oldest should be customer created (minus 10 days)
        assertThat(content.get(content.size() - 1).getType()).isEqualTo("CUSTOMER_CREATED");
    }

    @Test
    void getDealTimeline_AggregatesDealMilestonesAndActivities() {
        Pageable pageable = PageRequest.of(0, 10);

        when(dealRepository.findByIdAndDeletedFalse(dealId)).thenReturn(Optional.of(deal));
        when(activityRepository.findAllByDealOrderByCreatedAtDesc(deal))
                .thenReturn(Collections.singletonList(activity));

        Page<TimelineItemResponse> timeline = timelineService.getDealTimeline(dealId, userEmail, pageable);

        assertThat(timeline).isNotNull();
        // 1 deal created + 1 deal won + 1 activity = 3 items
        assertThat(timeline.getContent()).hasSize(3);
        verify(workspaceSecurityService).getMembershipOrThrow(workspaceId, userEmail);
    }

    @Test
    void getWorkspaceTimeline_AggregatesWorkspaceEvents() {
        Pageable pageable = PageRequest.of(0, 10);

        when(workspaceService.resolveWorkspace(workspaceId, userEmail)).thenReturn(organization);
        when(activityRepository.findAllByOrganizationOrderByCreatedAtDesc(organization))
                .thenReturn(Collections.singletonList(activity));
        when(dealRepository.findAllByOrganizationAndDeletedFalse(eq(organization), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(deal)));

        Page<TimelineItemResponse> timeline = timelineService.getWorkspaceTimeline(workspaceId, userEmail, pageable);

        assertThat(timeline).isNotNull();
        assertThat(timeline.getContent()).hasSize(2);
        verify(workspaceSecurityService).getMembershipOrThrow(workspaceId, userEmail);
    }
}
