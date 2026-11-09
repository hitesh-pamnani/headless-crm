package com.example.headlesscrm.service;

import com.example.headlesscrm.dto.TimelineItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TimelineService {
    Page<TimelineItemResponse> getCustomerTimeline(UUID customerId, String userEmail, Pageable pageable);
    Page<TimelineItemResponse> getDealTimeline(UUID dealId, String userEmail, Pageable pageable);
    Page<TimelineItemResponse> getWorkspaceTimeline(UUID workspaceId, String userEmail, Pageable pageable);
}
