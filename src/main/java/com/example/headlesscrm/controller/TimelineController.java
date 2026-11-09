package com.example.headlesscrm.controller;

import com.example.headlesscrm.dto.TimelineItemResponse;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.TimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/timeline")
@RequiredArgsConstructor
@Tag(name = "Timeline", description = "Aggregate relationship history and interaction timeline endpoints")
public class TimelineController {

    private final TimelineService timelineService;

    @GetMapping
    @Operation(summary = "Get workspace timeline", description = "Retrieves an aggregated chronological feed of all activities, deals, and milestones across the workspace.")
    public ResponseEntity<Page<TimelineItemResponse>> getWorkspaceTimeline(
            @Parameter(description = "Optional workspace UUID header. Defaults to user's primary workspace.")
            @RequestHeader(value = "X-Workspace-Id", required = false) UUID workspaceId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TimelineItemResponse> response = timelineService.getWorkspaceTimeline(workspaceId, userDetails.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customers/{customerId}")
    @Operation(summary = "Get customer timeline", description = "Retrieves an aggregated chronological feed of all interactions, notes, calls, meetings, contacts, and deals linked to a customer.")
    public ResponseEntity<Page<TimelineItemResponse>> getCustomerTimeline(
            @PathVariable UUID customerId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TimelineItemResponse> response = timelineService.getCustomerTimeline(customerId, userDetails.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/deals/{dealId}")
    @Operation(summary = "Get deal timeline", description = "Retrieves an aggregated chronological interaction feed of notes, calls, meetings, and stage changes for a specific deal.")
    public ResponseEntity<Page<TimelineItemResponse>> getDealTimeline(
            @PathVariable UUID dealId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TimelineItemResponse> response = timelineService.getDealTimeline(dealId, userDetails.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }
}
