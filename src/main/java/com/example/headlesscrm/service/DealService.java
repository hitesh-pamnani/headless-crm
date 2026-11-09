package com.example.headlesscrm.service;

import com.example.headlesscrm.domain.Deal;
import com.example.headlesscrm.domain.DealStatus;
import com.example.headlesscrm.dto.DealRequest;
import com.example.headlesscrm.dto.DealResponse;
import com.example.headlesscrm.dto.DealStageUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DealService {
    DealResponse createDeal(UUID workspaceId, DealRequest request, String userEmail);
    Page<DealResponse> getDeals(UUID workspaceId, UUID pipelineId, UUID stageId, UUID customerId, DealStatus status, String userEmail, Pageable pageable);
    DealResponse getDealById(UUID dealId, String userEmail);
    Deal getDeal(UUID dealId, String userEmail);
    DealResponse updateDeal(UUID dealId, DealRequest request, String userEmail);
    DealResponse updateDealStage(UUID dealId, DealStageUpdateRequest request, String userEmail);
    void deleteDeal(UUID dealId, String userEmail);
}
