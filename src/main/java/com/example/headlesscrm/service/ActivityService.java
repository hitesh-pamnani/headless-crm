package com.example.headlesscrm.service;

import com.example.headlesscrm.domain.ActivityStatus;
import com.example.headlesscrm.domain.ActivityType;
import com.example.headlesscrm.dto.ActivityRequest;
import com.example.headlesscrm.dto.ActivityResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ActivityService {
    ActivityResponse createActivity(UUID workspaceId, ActivityRequest request, String userEmail);
    Page<ActivityResponse> getActivities(UUID workspaceId, ActivityType type, ActivityStatus status, UUID customerId, UUID contactId, UUID dealId, String userEmail, Pageable pageable);
    ActivityResponse getActivityById(UUID activityId, String userEmail);
    ActivityResponse updateActivity(UUID activityId, ActivityRequest request, String userEmail);
    ActivityResponse completeActivity(UUID activityId, String userEmail);
    void deleteActivity(UUID activityId, String userEmail);
}
