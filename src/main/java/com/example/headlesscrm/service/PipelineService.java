package com.example.headlesscrm.service;

import com.example.headlesscrm.domain.Organization;
import com.example.headlesscrm.domain.Pipeline;
import com.example.headlesscrm.dto.PipelineRequest;
import com.example.headlesscrm.dto.PipelineResponse;

import java.util.List;
import java.util.UUID;

public interface PipelineService {
    PipelineResponse createPipeline(UUID workspaceId, PipelineRequest request, String userEmail);
    List<PipelineResponse> getPipelines(UUID workspaceId, String userEmail);
    PipelineResponse getPipelineById(UUID pipelineId, String userEmail);
    PipelineResponse updatePipeline(UUID pipelineId, PipelineRequest request, String userEmail);
    void deletePipeline(UUID pipelineId, String userEmail);
    PipelineResponse setDefaultPipeline(UUID pipelineId, String userEmail);
    Pipeline getOrCreateDefaultPipeline(Organization organization);
}
