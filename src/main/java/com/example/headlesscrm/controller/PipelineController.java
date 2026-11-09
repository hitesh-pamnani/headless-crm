package com.example.headlesscrm.controller;

import com.example.headlesscrm.dto.PipelineRequest;
import com.example.headlesscrm.dto.PipelineResponse;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.PipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/pipelines")
@RequiredArgsConstructor
@Tag(name = "Pipelines", description = "Sales pipeline and stage configuration endpoints")
public class PipelineController {

    private final PipelineService pipelineService;

    @PostMapping
    @Operation(summary = "Create pipeline", description = "Creates a new configurable sales pipeline with custom stages in the workspace.")
    public ResponseEntity<PipelineResponse> createPipeline(
            @Parameter(description = "Optional workspace UUID header. Defaults to user's primary workspace.")
            @RequestHeader(value = "X-Workspace-Id", required = false) UUID workspaceId,
            @Valid @RequestBody PipelineRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PipelineResponse response = pipelineService.createPipeline(workspaceId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List pipelines", description = "Retrieves all sales pipelines configured in the workspace.")
    public ResponseEntity<List<PipelineResponse>> getPipelines(
            @Parameter(description = "Optional workspace UUID header. Defaults to user's primary workspace.")
            @RequestHeader(value = "X-Workspace-Id", required = false) UUID workspaceId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<PipelineResponse> response = pipelineService.getPipelines(workspaceId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pipeline by ID", description = "Retrieves a pipeline with all its stages by ID.")
    public ResponseEntity<PipelineResponse> getPipelineById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PipelineResponse response = pipelineService.getPipelineById(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update pipeline", description = "Updates a pipeline's metadata and stage configurations.")
    public ResponseEntity<PipelineResponse> updatePipeline(
            @PathVariable UUID id,
            @Valid @RequestBody PipelineRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PipelineResponse response = pipelineService.updatePipeline(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete pipeline", description = "Deletes a pipeline. Default pipeline and pipelines with active deals cannot be deleted.")
    public ResponseEntity<Void> deletePipeline(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        pipelineService.deletePipeline(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/set-default")
    @Operation(summary = "Set default pipeline", description = "Designates a pipeline as the workspace default.")
    public ResponseEntity<PipelineResponse> setDefaultPipeline(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PipelineResponse response = pipelineService.setDefaultPipeline(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
