package com.example.headlesscrm.controller;

import com.example.headlesscrm.domain.ActivityStatus;
import com.example.headlesscrm.domain.ActivityType;
import com.example.headlesscrm.dto.ActivityRequest;
import com.example.headlesscrm.dto.ActivityResponse;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.ActivityService;
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
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Tag(name = "Activities", description = "Interaction logging endpoints for calls, meetings, notes, and tasks")
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping
    @Operation(summary = "Log activity or create task", description = "Creates an activity (CALL, MEETING, NOTE, TASK) linked to customers, contacts, or deals.")
    public ResponseEntity<ActivityResponse> createActivity(
            @Parameter(description = "Optional workspace UUID header. Defaults to user's primary workspace.")
            @RequestHeader(value = "X-Workspace-Id", required = false) UUID workspaceId,
            @Valid @RequestBody ActivityRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ActivityResponse response = activityService.createActivity(workspaceId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List activities", description = "Retrieves a paginated list of logged activities with optional filtering by type, status, customer, contact, and deal.")
    public ResponseEntity<Page<ActivityResponse>> getActivities(
            @Parameter(description = "Optional workspace UUID header. Defaults to user's primary workspace.")
            @RequestHeader(value = "X-Workspace-Id", required = false) UUID workspaceId,
            @Parameter(description = "Filter by activity type (CALL, MEETING, NOTE, TASK)")
            @RequestParam(required = false) ActivityType type,
            @Parameter(description = "Filter by activity status (PENDING, COMPLETED, CANCELLED)")
            @RequestParam(required = false) ActivityStatus status,
            @Parameter(description = "Filter by customer UUID")
            @RequestParam(required = false) UUID customerId,
            @Parameter(description = "Filter by contact UUID")
            @RequestParam(required = false) UUID contactId,
            @Parameter(description = "Filter by deal UUID")
            @RequestParam(required = false) UUID dealId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ActivityResponse> response = activityService.getActivities(workspaceId, type, status, customerId, contactId, dealId, userDetails.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get activity by ID", description = "Retrieves details of a specific logged activity or task.")
    public ResponseEntity<ActivityResponse> getActivityById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ActivityResponse response = activityService.getActivityById(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update activity", description = "Updates details, notes, or scheduling of an existing activity.")
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable UUID id,
            @Valid @RequestBody ActivityRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ActivityResponse response = activityService.updateActivity(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Complete activity", description = "Marks an activity or task as completed with current timestamp.")
    public ResponseEntity<ActivityResponse> completeActivity(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ActivityResponse response = activityService.completeActivity(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete activity", description = "Deletes an activity. Requires ADMIN or OWNER role.")
    public ResponseEntity<Void> deleteActivity(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        activityService.deleteActivity(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
