package com.example.headlesscrm.service;

import com.example.headlesscrm.domain.*;
import com.example.headlesscrm.dto.DealRequest;
import com.example.headlesscrm.dto.DealResponse;
import com.example.headlesscrm.dto.DealStageUpdateRequest;
import com.example.headlesscrm.exception.ResourceNotFoundException;
import com.example.headlesscrm.repository.*;
import com.example.headlesscrm.security.WorkspaceSecurityService;
import com.example.headlesscrm.service.impl.DealServiceImpl;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DealServiceImplTest {

    @Mock
    private DealRepository dealRepository;

    @Mock
    private PipelineRepository pipelineRepository;

    @Mock
    private PipelineStageRepository pipelineStageRepository;

    @Mock
    private PipelineService pipelineService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private WorkspaceSecurityService workspaceSecurityService;

    @InjectMocks
    private DealServiceImpl dealService;

    private Organization organization;
    private User user;
    private Customer customer;
    private Contact contact;
    private Pipeline pipeline;
    private PipelineStage stageOpen;
    private PipelineStage stageWon;
    private PipelineStage stageLost;
    private Deal deal;
    private UUID workspaceId;
    private UUID dealId;
    private final String userEmail = "sales@example.com";

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        dealId = UUID.randomUUID();

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
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .organization(organization)
                .build();

        contact = Contact.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .customer(customer)
                .build();

        pipeline = Pipeline.builder()
                .id(UUID.randomUUID())
                .name("Standard Sales Pipeline")
                .isDefault(true)
                .organization(organization)
                .stages(new ArrayList<>())
                .build();

        stageOpen = PipelineStage.builder()
                .id(UUID.randomUUID())
                .name("Lead")
                .displayOrder(0)
                .probability(10)
                .category(StageCategory.OPEN)
                .pipeline(pipeline)
                .build();

        stageWon = PipelineStage.builder()
                .id(UUID.randomUUID())
                .name("Won")
                .displayOrder(1)
                .probability(100)
                .category(StageCategory.WON)
                .pipeline(pipeline)
                .build();

        stageLost = PipelineStage.builder()
                .id(UUID.randomUUID())
                .name("Lost")
                .displayOrder(2)
                .probability(0)
                .category(StageCategory.LOST)
                .pipeline(pipeline)
                .build();

        pipeline.addStage(stageOpen);
        pipeline.addStage(stageWon);
        pipeline.addStage(stageLost);

        deal = Deal.builder()
                .id(dealId)
                .title("Big Enterprise Contract")
                .value(new BigDecimal("75000.00"))
                .currency("USD")
                .status(DealStatus.OPEN)
                .pipeline(pipeline)
                .stage(stageOpen)
                .organization(organization)
                .customer(customer)
                .contact(contact)
                .owner(user)
                .customFields(new HashMap<>())
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createDeal_Success_WithCustomerAndContact() {
        DealRequest request = DealRequest.builder()
                .title("Big Enterprise Contract")
                .value(new BigDecimal("75000.00"))
                .currency("USD")
                .expectedCloseDate(LocalDate.of(2026, 12, 31))
                .customerId(customer.getId())
                .contactId(contact.getId())
                .build();

        when(workspaceService.resolveWorkspace(workspaceId, userEmail)).thenReturn(organization);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(pipelineService.getOrCreateDefaultPipeline(organization)).thenReturn(pipeline);
        when(customerRepository.findByIdAndOrganizationAndDeletedFalse(customer.getId(), organization)).thenReturn(Optional.of(customer));
        when(contactRepository.findByIdAndCustomer(contact.getId(), customer)).thenReturn(Optional.of(contact));
        when(dealRepository.save(any(Deal.class))).thenAnswer(inv -> {
            Deal d = inv.getArgument(0);
            d.setId(dealId);
            return d;
        });

        DealResponse response = dealService.createDeal(workspaceId, request, userEmail);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Big Enterprise Contract");
        assertThat(response.getValue()).isEqualTo(new BigDecimal("75000.00"));
        assertThat(response.getStatus()).isEqualTo(DealStatus.OPEN);
        assertThat(response.getCustomerId()).isEqualTo(customer.getId());
        assertThat(response.getContactId()).isEqualTo(contact.getId());
        verify(workspaceSecurityService).validateCanModify(workspaceId, userEmail);
    }

    @Test
    void createDeal_ReadOnlyMember_ThrowsAccessDenied() {
        DealRequest request = DealRequest.builder().title("Contract").build();
        when(workspaceService.resolveWorkspace(workspaceId, userEmail)).thenReturn(organization);
        doThrow(new AccessDeniedException("Read-only members are not permitted to modify workspace data."))
                .when(workspaceSecurityService).validateCanModify(workspaceId, userEmail);

        assertThatThrownBy(() -> dealService.createDeal(workspaceId, request, userEmail))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getDeals_Filtered_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Deal> dealPage = new PageImpl<>(Collections.singletonList(deal), pageable, 1);

        when(workspaceService.resolveWorkspace(workspaceId, userEmail)).thenReturn(organization);
        when(dealRepository.findFiltered(organization, null, null, null, null, pageable)).thenReturn(dealPage);

        Page<DealResponse> result = dealService.getDeals(workspaceId, null, null, null, null, userEmail, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Big Enterprise Contract");
        verify(workspaceSecurityService).getMembershipOrThrow(workspaceId, userEmail);
    }

    @Test
    void getDealById_Success() {
        when(dealRepository.findByIdAndDeletedFalse(dealId)).thenReturn(Optional.of(deal));

        DealResponse response = dealService.getDealById(dealId, userEmail);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(dealId);
        verify(workspaceSecurityService).getMembershipOrThrow(workspaceId, userEmail);
    }

    @Test
    void updateDeal_Success() {
        DealRequest request = DealRequest.builder()
                .title("Updated Contract Title")
                .value(new BigDecimal("90000.00"))
                .currency("EUR")
                .build();

        when(dealRepository.findByIdAndDeletedFalse(dealId)).thenReturn(Optional.of(deal));
        when(dealRepository.save(any(Deal.class))).thenAnswer(inv -> inv.getArgument(0));

        DealResponse response = dealService.updateDeal(dealId, request, userEmail);

        assertThat(response.getTitle()).isEqualTo("Updated Contract Title");
        assertThat(response.getValue()).isEqualTo(new BigDecimal("90000.00"));
        assertThat(response.getCurrency()).isEqualTo("EUR");
        verify(workspaceSecurityService).validateCanModify(workspaceId, userEmail);
    }

    @Test
    void updateDealStage_AdvancesStage_SetsWonAndActualCloseDate() {
        DealStageUpdateRequest request = DealStageUpdateRequest.builder()
                .stageId(stageWon.getId())
                .build();

        when(dealRepository.findByIdAndDeletedFalse(dealId)).thenReturn(Optional.of(deal));
        when(pipelineStageRepository.findByIdAndPipeline(stageWon.getId(), pipeline)).thenReturn(Optional.of(stageWon));
        when(dealRepository.save(any(Deal.class))).thenAnswer(inv -> inv.getArgument(0));

        DealResponse response = dealService.updateDealStage(dealId, request, userEmail);

        assertThat(response.getStatus()).isEqualTo(DealStatus.WON);
        assertThat(response.getStageName()).isEqualTo("Won");
        assertThat(response.getActualCloseDate()).isNotNull();
    }

    @Test
    void updateDealStage_MovesToLost_SetsLostAndActualCloseDate() {
        DealStageUpdateRequest request = DealStageUpdateRequest.builder()
                .stageId(stageLost.getId())
                .build();

        when(dealRepository.findByIdAndDeletedFalse(dealId)).thenReturn(Optional.of(deal));
        when(pipelineStageRepository.findByIdAndPipeline(stageLost.getId(), pipeline)).thenReturn(Optional.of(stageLost));
        when(dealRepository.save(any(Deal.class))).thenAnswer(inv -> inv.getArgument(0));

        DealResponse response = dealService.updateDealStage(dealId, request, userEmail);

        assertThat(response.getStatus()).isEqualTo(DealStatus.LOST);
        assertThat(response.getStageName()).isEqualTo("Lost");
        assertThat(response.getActualCloseDate()).isNotNull();
    }

    @Test
    void updateDealStage_MovesToOpen_ClearsActualCloseDate() {
        deal.setStatus(DealStatus.WON);
        deal.setActualCloseDate(LocalDateTime.now());

        DealStageUpdateRequest request = DealStageUpdateRequest.builder()
                .stageId(stageOpen.getId())
                .build();

        when(dealRepository.findByIdAndDeletedFalse(dealId)).thenReturn(Optional.of(deal));
        when(pipelineStageRepository.findByIdAndPipeline(stageOpen.getId(), pipeline)).thenReturn(Optional.of(stageOpen));
        when(dealRepository.save(any(Deal.class))).thenAnswer(inv -> inv.getArgument(0));

        DealResponse response = dealService.updateDealStage(dealId, request, userEmail);

        assertThat(response.getStatus()).isEqualTo(DealStatus.OPEN);
        assertThat(response.getActualCloseDate()).isNull();
    }

    @Test
    void deleteDeal_NonAdmin_ThrowsAccessDenied() {
        when(dealRepository.findByIdAndDeletedFalse(dealId)).thenReturn(Optional.of(deal));
        doThrow(new AccessDeniedException("Only workspace Admins or Owners are permitted to perform delete actions."))
                .when(workspaceSecurityService).validateCanDelete(workspaceId, userEmail);

        assertThatThrownBy(() -> dealService.deleteDeal(dealId, userEmail))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteDeal_AdminOrOwner_Success_SoftDeletes() {
        when(dealRepository.findByIdAndDeletedFalse(dealId)).thenReturn(Optional.of(deal));
        when(dealRepository.save(any(Deal.class))).thenAnswer(inv -> inv.getArgument(0));

        dealService.deleteDeal(dealId, userEmail);

        assertThat(deal.isDeleted()).isTrue();
        assertThat(deal.getDeletedAt()).isNotNull();
        verify(workspaceSecurityService).validateCanDelete(workspaceId, userEmail);
    }
}
