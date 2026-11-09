package com.example.headlesscrm.controller;

import com.example.headlesscrm.domain.DealStatus;
import com.example.headlesscrm.dto.DealRequest;
import com.example.headlesscrm.dto.DealResponse;
import com.example.headlesscrm.dto.DealStageUpdateRequest;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.DealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
@Tag(name = "Deals", description = "Sales deal and opportunity management endpoints with pipeline tracking")
public class DealController {

    private final DealService dealService;

    @PostMapping
    @Operation(summary = "Create deal", description = "Creates a new deal associated with a customer, contact, pipeline, and stage in the workspace.")
    public ResponseEntity<DealResponse> createDeal(
            @Parameter(description = "Optional workspace UUID header. Defaults to user's primary workspace.")
            @RequestHeader(value = "X-Workspace-Id", required = false) UUID workspaceId,
            @Valid @RequestBody DealRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        DealResponse response = dealService.createDeal(workspaceId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List deals", description = "Retrieves a paginated list of deals in the workspace, with optional filtering by pipeline, stage, customer, and status.")
    public ResponseEntity<Page<DealResponse>> getDeals(
            @Parameter(description = "Optional workspace UUID header. Defaults to user's primary workspace.")
            @RequestHeader(value = "X-Workspace-Id", required = false) UUID workspaceId,
            @Parameter(description = "Filter by pipeline UUID")
            @RequestParam(required = false) UUID pipelineId,
            @Parameter(description = "Filter by stage UUID")
            @RequestParam(required = false) UUID stageId,
            @Parameter(description = "Filter by customer UUID")
            @RequestParam(required = false) UUID customerId,
            @Parameter(description = "Filter by deal status (OPEN, WON, LOST)")
            @RequestParam(required = false) DealStatus status,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<DealResponse> response = dealService.getDeals(workspaceId, pipelineId, stageId, customerId, status, userDetails.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get deal by ID", description = "Retrieves full details of a specific deal by ID.")
    public ResponseEntity<DealResponse> getDealById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        DealResponse response = dealService.getDealById(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update deal", description = "Updates details of an existing deal. Requires MEMBER, ADMIN, or OWNER role.")
    public ResponseEntity<DealResponse> updateDeal(
            @PathVariable UUID id,
            @Valid @RequestBody DealRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        DealResponse response = dealService.updateDeal(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/stage")
    @Operation(summary = "Move deal stage", description = "Advances or moves a deal to a new stage in its pipeline. Updates win/loss status automatically based on the stage category.")
    public ResponseEntity<DealResponse> updateDealStage(
            @PathVariable UUID id,
            @Valid @RequestBody DealStageUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        DealResponse response = dealService.updateDealStage(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete deal", description = "Marks a deal as deleted. Requires ADMIN or OWNER role.")
    public ResponseEntity<Void> deleteDeal(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        dealService.deleteDeal(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
