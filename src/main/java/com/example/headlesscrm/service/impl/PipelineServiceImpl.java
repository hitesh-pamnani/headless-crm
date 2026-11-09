package com.example.headlesscrm.service.impl;

import com.example.headlesscrm.domain.Organization;
import com.example.headlesscrm.domain.Pipeline;
import com.example.headlesscrm.domain.PipelineStage;
import com.example.headlesscrm.domain.StageCategory;
import com.example.headlesscrm.dto.PipelineRequest;
import com.example.headlesscrm.dto.PipelineResponse;
import com.example.headlesscrm.dto.PipelineStageRequest;
import com.example.headlesscrm.dto.PipelineStageResponse;
import com.example.headlesscrm.exception.BadRequestException;
import com.example.headlesscrm.exception.DuplicateResourceException;
import com.example.headlesscrm.exception.ResourceNotFoundException;
import com.example.headlesscrm.repository.DealRepository;
import com.example.headlesscrm.repository.PipelineRepository;
import com.example.headlesscrm.security.WorkspaceSecurityService;
import com.example.headlesscrm.service.PipelineService;
import com.example.headlesscrm.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PipelineServiceImpl implements PipelineService {

    private final PipelineRepository pipelineRepository;
    private final DealRepository dealRepository;
    private final WorkspaceService workspaceService;
    private final WorkspaceSecurityService workspaceSecurityService;

    @Override
    @Transactional
    public PipelineResponse createPipeline(UUID workspaceId, PipelineRequest request, String userEmail) {
        Organization organization = workspaceService.resolveWorkspace(workspaceId, userEmail);
        workspaceSecurityService.validateCanModify(organization.getId(), userEmail);

        if (pipelineRepository.existsByNameAndOrganization(request.getName().trim(), organization)) {
            throw new DuplicateResourceException("Pipeline with name '" + request.getName() + "' already exists in this workspace");
        }

        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault())
                || pipelineRepository.findAllByOrganization(organization).isEmpty();

        if (makeDefault) {
            clearPreviousDefault(organization);
        }

        Pipeline pipeline = Pipeline.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .isDefault(makeDefault)
                .organization(organization)
                .stages(new ArrayList<>())
                .build();

        if (request.getStages() != null && !request.getStages().isEmpty()) {
            for (PipelineStageRequest stageReq : request.getStages()) {
                PipelineStage stage = PipelineStage.builder()
                        .name(stageReq.getName().trim())
                        .displayOrder(stageReq.getDisplayOrder())
                        .probability(stageReq.getProbability())
                        .category(stageReq.getCategory() != null ? stageReq.getCategory() : StageCategory.OPEN)
                        .pipeline(pipeline)
                        .build();
                pipeline.addStage(stage);
            }
        } else {
            createDefaultStagesForPipeline(pipeline);
        }

        Pipeline saved = pipelineRepository.save(pipeline);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public List<PipelineResponse> getPipelines(UUID workspaceId, String userEmail) {
        Organization organization = workspaceService.resolveWorkspace(workspaceId, userEmail);
        workspaceSecurityService.getMembershipOrThrow(organization.getId(), userEmail);

        List<Pipeline> pipelines = pipelineRepository.findAllByOrganization(organization);
        if (pipelines.isEmpty()) {
            pipelines = Collections.singletonList(getOrCreateDefaultPipeline(organization));
        }

        return pipelines.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PipelineResponse getPipelineById(UUID pipelineId, String userEmail) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found with id: " + pipelineId));

        workspaceSecurityService.getMembershipOrThrow(pipeline.getOrganization().getId(), userEmail);
        return mapToResponse(pipeline);
    }

    @Override
    @Transactional
    public PipelineResponse updatePipeline(UUID pipelineId, PipelineRequest request, String userEmail) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found with id: " + pipelineId));

        workspaceSecurityService.validateCanModify(pipeline.getOrganization().getId(), userEmail);

        if (!pipeline.getName().equalsIgnoreCase(request.getName().trim())
                && pipelineRepository.existsByNameAndOrganizationAndIdNot(request.getName().trim(), pipeline.getOrganization(), pipelineId)) {
            throw new DuplicateResourceException("Pipeline with name '" + request.getName() + "' already exists in this workspace");
        }

        pipeline.setName(request.getName().trim());
        pipeline.setDescription(request.getDescription());

        if (Boolean.TRUE.equals(request.getIsDefault()) && !pipeline.isDefault()) {
            clearPreviousDefault(pipeline.getOrganization());
            pipeline.setDefault(true);
        }

        if (request.getStages() != null && !request.getStages().isEmpty()) {
            pipeline.getStages().clear();
            for (PipelineStageRequest stageReq : request.getStages()) {
                PipelineStage stage = PipelineStage.builder()
                        .name(stageReq.getName().trim())
                        .displayOrder(stageReq.getDisplayOrder())
                        .probability(stageReq.getProbability())
                        .category(stageReq.getCategory() != null ? stageReq.getCategory() : StageCategory.OPEN)
                        .pipeline(pipeline)
                        .build();
                pipeline.addStage(stage);
            }
        }

        Pipeline updated = pipelineRepository.save(pipeline);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deletePipeline(UUID pipelineId, String userEmail) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found with id: " + pipelineId));

        workspaceSecurityService.validateCanDelete(pipeline.getOrganization().getId(), userEmail);

        if (pipeline.isDefault()) {
            throw new BadRequestException("Cannot delete the default workspace pipeline");
        }

        if (dealRepository.existsByPipelineAndDeletedFalse(pipeline)) {
            throw new BadRequestException("Cannot delete pipeline that contains active deals");
        }

        pipelineRepository.delete(pipeline);
    }

    @Override
    @Transactional
    public PipelineResponse setDefaultPipeline(UUID pipelineId, String userEmail) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found with id: " + pipelineId));

        workspaceSecurityService.validateCanModify(pipeline.getOrganization().getId(), userEmail);

        clearPreviousDefault(pipeline.getOrganization());
        pipeline.setDefault(true);
        Pipeline saved = pipelineRepository.save(pipeline);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public Pipeline getOrCreateDefaultPipeline(Organization organization) {
        return pipelineRepository.findByOrganizationAndIsDefaultTrue(organization)
                .orElseGet(() -> {
                    List<Pipeline> existing = pipelineRepository.findAllByOrganization(organization);
                    if (!existing.isEmpty()) {
                        Pipeline first = existing.get(0);
                        first.setDefault(true);
                        return pipelineRepository.save(first);
                    }

                    Pipeline defaultPipeline = Pipeline.builder()
                            .name("Standard Sales Pipeline")
                            .description("Default sales pipeline with standard deal stages")
                            .isDefault(true)
                            .organization(organization)
                            .stages(new ArrayList<>())
                            .build();

                    createDefaultStagesForPipeline(defaultPipeline);
                    return pipelineRepository.save(defaultPipeline);
                });
    }

    private void clearPreviousDefault(Organization organization) {
        pipelineRepository.findByOrganizationAndIsDefaultTrue(organization).ifPresent(p -> {
            p.setDefault(false);
            pipelineRepository.save(p);
        });
    }

    private void createDefaultStagesForPipeline(Pipeline pipeline) {
        pipeline.addStage(PipelineStage.builder().name("Lead").displayOrder(0).probability(10).category(StageCategory.OPEN).build());
        pipeline.addStage(PipelineStage.builder().name("Qualified").displayOrder(1).probability(30).category(StageCategory.OPEN).build());
        pipeline.addStage(PipelineStage.builder().name("Proposal").displayOrder(2).probability(60).category(StageCategory.OPEN).build());
        pipeline.addStage(PipelineStage.builder().name("Negotiation").displayOrder(3).probability(80).category(StageCategory.OPEN).build());
        pipeline.addStage(PipelineStage.builder().name("Won").displayOrder(4).probability(100).category(StageCategory.WON).build());
        pipeline.addStage(PipelineStage.builder().name("Lost").displayOrder(5).probability(0).category(StageCategory.LOST).build());
    }

    private PipelineResponse mapToResponse(Pipeline pipeline) {
        List<PipelineStageResponse> stages = pipeline.getStages().stream()
                .sorted(Comparator.comparingInt(PipelineStage::getDisplayOrder))
                .map(stage -> PipelineStageResponse.builder()
                        .id(stage.getId())
                        .name(stage.getName())
                        .displayOrder(stage.getDisplayOrder())
                        .probability(stage.getProbability())
                        .category(stage.getCategory())
                        .createdAt(stage.getCreatedAt())
                        .updatedAt(stage.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return PipelineResponse.builder()
                .id(pipeline.getId())
                .name(pipeline.getName())
                .description(pipeline.getDescription())
                .isDefault(pipeline.isDefault())
                .organizationId(pipeline.getOrganization() != null ? pipeline.getOrganization().getId() : null)
                .stages(stages)
                .createdAt(pipeline.getCreatedAt())
                .updatedAt(pipeline.getUpdatedAt())
                .build();
    }
}
