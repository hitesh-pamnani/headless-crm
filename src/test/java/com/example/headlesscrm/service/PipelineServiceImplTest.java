package com.example.headlesscrm.service;

import com.example.headlesscrm.domain.*;
import com.example.headlesscrm.dto.PipelineRequest;
import com.example.headlesscrm.dto.PipelineResponse;
import com.example.headlesscrm.dto.PipelineStageRequest;
import com.example.headlesscrm.exception.BadRequestException;
import com.example.headlesscrm.exception.DuplicateResourceException;
import com.example.headlesscrm.exception.ResourceNotFoundException;
import com.example.headlesscrm.repository.DealRepository;
import com.example.headlesscrm.repository.PipelineRepository;
import com.example.headlesscrm.security.WorkspaceSecurityService;
import com.example.headlesscrm.service.impl.PipelineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineServiceImplTest {

    @Mock
    private PipelineRepository pipelineRepository;

    @Mock
    private DealRepository dealRepository;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private WorkspaceSecurityService workspaceSecurityService;

    @InjectMocks
    private PipelineServiceImpl pipelineService;

    private Organization organization;
    private Pipeline pipeline;
    private PipelineStage stageLead;
    private PipelineStage stageWon;
    private UUID pipelineId;
    private UUID workspaceId;
    private final String userEmail = "admin@example.com";

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        pipelineId = UUID.randomUUID();

        organization = Organization.builder()
                .id(workspaceId)
                .name("Acme Corp")
                .slug("acme-corp")
                .build();

        pipeline = Pipeline.builder()
                .id(pipelineId)
                .name("Sales Pipeline")
                .description("Primary outbound pipeline")
                .isDefault(true)
                .organization(organization)
                .stages(new ArrayList<>())
                .build();

        stageLead = PipelineStage.builder()
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

        pipeline.addStage(stageLead);
        pipeline.addStage(stageWon);
    }

    @Test
    void createPipeline_Success() {
        PipelineStageRequest stageReq = PipelineStageRequest.builder()
                .name("Inbound")
                .displayOrder(0)
                .probability(20)
                .category(StageCategory.OPEN)
                .build();

        PipelineRequest request = PipelineRequest.builder()
                .name("Inbound Pipeline")
                .description("Inbound lead flow")
                .isDefault(false)
                .stages(Collections.singletonList(stageReq))
                .build();

        when(workspaceService.resolveWorkspace(workspaceId, userEmail)).thenReturn(organization);
        when(pipelineRepository.existsByNameAndOrganization("Inbound Pipeline", organization)).thenReturn(false);
        when(pipelineRepository.findAllByOrganization(organization)).thenReturn(Collections.singletonList(pipeline));
        when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(invocation -> {
            Pipeline p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PipelineResponse response = pipelineService.createPipeline(workspaceId, request, userEmail);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Inbound Pipeline");
        assertThat(response.getStages()).hasSize(1);
        assertThat(response.getStages().get(0).getName()).isEqualTo("Inbound");
        verify(workspaceSecurityService).validateCanModify(workspaceId, userEmail);
    }

    @Test
    void createPipeline_DuplicateName_ThrowsException() {
        PipelineRequest request = PipelineRequest.builder()
                .name("Sales Pipeline")
                .build();

        when(workspaceService.resolveWorkspace(workspaceId, userEmail)).thenReturn(organization);
        when(pipelineRepository.existsByNameAndOrganization("Sales Pipeline", organization)).thenReturn(true);

        assertThatThrownBy(() -> pipelineService.createPipeline(workspaceId, request, userEmail))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Sales Pipeline");
    }

    @Test
    void getPipelines_ReturnsExistingPipelines() {
        when(workspaceService.resolveWorkspace(workspaceId, userEmail)).thenReturn(organization);
        when(pipelineRepository.findAllByOrganization(organization)).thenReturn(Collections.singletonList(pipeline));

        List<PipelineResponse> responses = pipelineService.getPipelines(workspaceId, userEmail);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Sales Pipeline");
        assertThat(responses.get(0).getStages()).hasSize(2);
    }

    @Test
    void getPipelines_WhenEmpty_CreatesDefaultPipeline() {
        when(workspaceService.resolveWorkspace(workspaceId, userEmail)).thenReturn(organization);
        when(pipelineRepository.findAllByOrganization(organization)).thenReturn(Collections.emptyList());
        when(pipelineRepository.findByOrganizationAndIsDefaultTrue(organization)).thenReturn(Optional.empty());
        when(pipelineRepository.save(any(Pipeline.class))).thenReturn(pipeline);

        List<PipelineResponse> responses = pipelineService.getPipelines(workspaceId, userEmail);

        assertThat(responses).hasSize(1);
        verify(pipelineRepository, atLeastOnce()).save(any(Pipeline.class));
    }

    @Test
    void getPipelineById_Success() {
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(pipeline));

        PipelineResponse response = pipelineService.getPipelineById(pipelineId, userEmail);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Sales Pipeline");
        verify(workspaceSecurityService).getMembershipOrThrow(workspaceId, userEmail);
    }

    @Test
    void getPipelineById_NotFound_ThrowsException() {
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pipelineService.getPipelineById(pipelineId, userEmail))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updatePipeline_Success() {
        PipelineRequest request = PipelineRequest.builder()
                .name("Updated Pipeline")
                .description("New description")
                .stages(Collections.emptyList())
                .build();

        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(pipeline));
        when(pipelineRepository.existsByNameAndOrganizationAndIdNot("Updated Pipeline", organization, pipelineId)).thenReturn(false);
        when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(inv -> inv.getArgument(0));

        PipelineResponse response = pipelineService.updatePipeline(pipelineId, request, userEmail);

        assertThat(response.getName()).isEqualTo("Updated Pipeline");
        assertThat(response.getDescription()).isEqualTo("New description");
        verify(workspaceSecurityService).validateCanModify(workspaceId, userEmail);
    }

    @Test
    void deletePipeline_DefaultPipeline_ThrowsBadRequest() {
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(pipeline));

        assertThatThrownBy(() -> pipelineService.deletePipeline(pipelineId, userEmail))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete the default workspace pipeline");
    }

    @Test
    void deletePipeline_WithActiveDeals_ThrowsBadRequest() {
        pipeline.setDefault(false);
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(pipeline));
        when(dealRepository.existsByPipelineAndDeletedFalse(pipeline)).thenReturn(true);

        assertThatThrownBy(() -> pipelineService.deletePipeline(pipelineId, userEmail))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("contains active deals");
    }

    @Test
    void deletePipeline_Success() {
        pipeline.setDefault(false);
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(pipeline));
        when(dealRepository.existsByPipelineAndDeletedFalse(pipeline)).thenReturn(false);

        pipelineService.deletePipeline(pipelineId, userEmail);

        verify(pipelineRepository).delete(pipeline);
        verify(workspaceSecurityService).validateCanDelete(workspaceId, userEmail);
    }

    @Test
    void setDefaultPipeline_Success() {
        Pipeline otherPipeline = Pipeline.builder()
                .id(UUID.randomUUID())
                .name("Second Pipeline")
                .isDefault(false)
                .organization(organization)
                .stages(new ArrayList<>())
                .build();

        when(pipelineRepository.findById(otherPipeline.getId())).thenReturn(Optional.of(otherPipeline));
        when(pipelineRepository.findByOrganizationAndIsDefaultTrue(organization)).thenReturn(Optional.of(pipeline));
        when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(inv -> inv.getArgument(0));

        PipelineResponse response = pipelineService.setDefaultPipeline(otherPipeline.getId(), userEmail);

        assertThat(response.isDefault()).isTrue();
        assertThat(pipeline.isDefault()).isFalse();
    }
}
