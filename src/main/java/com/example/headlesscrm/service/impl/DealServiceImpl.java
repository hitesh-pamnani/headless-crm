package com.example.headlesscrm.service.impl;

import com.example.headlesscrm.domain.*;
import com.example.headlesscrm.dto.DealRequest;
import com.example.headlesscrm.dto.DealResponse;
import com.example.headlesscrm.dto.DealStageUpdateRequest;
import com.example.headlesscrm.exception.BadRequestException;
import com.example.headlesscrm.exception.ResourceNotFoundException;
import com.example.headlesscrm.repository.*;
import com.example.headlesscrm.security.WorkspaceSecurityService;
import com.example.headlesscrm.service.DealService;
import com.example.headlesscrm.service.PipelineService;
import com.example.headlesscrm.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DealServiceImpl implements DealService {

    private final DealRepository dealRepository;
    private final PipelineRepository pipelineRepository;
    private final PipelineStageRepository pipelineStageRepository;
    private final PipelineService pipelineService;
    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;
    private final WorkspaceSecurityService workspaceSecurityService;

    @Override
    @Transactional
    public DealResponse createDeal(UUID workspaceId, DealRequest request, String userEmail) {
        Organization organization = workspaceService.resolveWorkspace(workspaceId, userEmail);
        workspaceSecurityService.validateCanModify(organization.getId(), userEmail);
        User owner = findUserByEmail(userEmail);

        Pipeline pipeline;
        if (request.getPipelineId() != null) {
            pipeline = pipelineRepository.findByIdAndOrganization(request.getPipelineId(), organization)
                    .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found with id: " + request.getPipelineId()));
        } else {
            pipeline = pipelineService.getOrCreateDefaultPipeline(organization);
        }

        PipelineStage stage;
        if (request.getStageId() != null) {
            stage = pipelineStageRepository.findByIdAndPipeline(request.getStageId(), pipeline)
                    .orElseThrow(() -> new ResourceNotFoundException("Stage not found with id: " + request.getStageId() + " in pipeline: " + pipeline.getId()));
        } else {
            if (pipeline.getStages().isEmpty()) {
                throw new BadRequestException("Pipeline has no configured stages");
            }
            stage = pipeline.getStages().get(0);
        }

        final Customer customer = request.getCustomerId() != null
                ? customerRepository.findByIdAndOrganizationAndDeletedFalse(request.getCustomerId(), organization)
                        .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()))
                : null;

        Contact contact = null;
        if (request.getContactId() != null) {
            if (customer != null) {
                contact = contactRepository.findByIdAndCustomer(request.getContactId(), customer)
                        .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + request.getContactId() + " for customer: " + customer.getId()));
            } else {
                contact = contactRepository.findById(request.getContactId())
                        .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + request.getContactId()));
                if (!contact.getCustomer().getOrganization().getId().equals(organization.getId())) {
                    throw new BadRequestException("Contact belongs to a different workspace");
                }
            }
        }

        DealStatus status = resolveDealStatus(request.getStatus(), stage);
        LocalDateTime actualCloseDate = (status == DealStatus.WON || status == DealStatus.LOST) ? LocalDateTime.now() : null;

        Deal deal = Deal.builder()
                .title(request.getTitle().trim())
                .value(request.getValue() != null ? request.getValue() : BigDecimal.ZERO)
                .currency(request.getCurrency() != null && !request.getCurrency().isBlank() ? request.getCurrency().trim().toUpperCase() : "USD")
                .status(status)
                .expectedCloseDate(request.getExpectedCloseDate())
                .actualCloseDate(actualCloseDate)
                .pipeline(pipeline)
                .stage(stage)
                .organization(organization)
                .customer(customer)
                .contact(contact)
                .owner(owner)
                .customFields(request.getCustomFields() != null ? request.getCustomFields() : new HashMap<>())
                .deleted(false)
                .build();

        Deal saved = dealRepository.save(deal);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DealResponse> getDeals(UUID workspaceId, UUID pipelineId, UUID stageId, UUID customerId, DealStatus status, String userEmail, Pageable pageable) {
        Organization organization = workspaceService.resolveWorkspace(workspaceId, userEmail);
        workspaceSecurityService.getMembershipOrThrow(organization.getId(), userEmail);

        return dealRepository.findFiltered(organization, pipelineId, stageId, customerId, status, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Deal getDeal(UUID dealId, String userEmail) {
        Deal deal = dealRepository.findByIdAndDeletedFalse(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found with id: " + dealId));

        workspaceSecurityService.getMembershipOrThrow(deal.getOrganization().getId(), userEmail);
        return deal;
    }

    @Override
    @Transactional(readOnly = true)
    public DealResponse getDealById(UUID dealId, String userEmail) {
        Deal deal = getDeal(dealId, userEmail);
        return mapToResponse(deal);
    }

    @Override
    @Transactional
    public DealResponse updateDeal(UUID dealId, DealRequest request, String userEmail) {
        Deal deal = getDeal(dealId, userEmail);
        workspaceSecurityService.validateCanModify(deal.getOrganization().getId(), userEmail);

        deal.setTitle(request.getTitle().trim());
        if (request.getValue() != null) {
            deal.setValue(request.getValue());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            deal.setCurrency(request.getCurrency().trim().toUpperCase());
        }
        if (request.getExpectedCloseDate() != null) {
            deal.setExpectedCloseDate(request.getExpectedCloseDate());
        }

        if (request.getPipelineId() != null && !request.getPipelineId().equals(deal.getPipeline().getId())) {
            Pipeline newPipeline = pipelineRepository.findByIdAndOrganization(request.getPipelineId(), deal.getOrganization())
                    .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found with id: " + request.getPipelineId()));
            deal.setPipeline(newPipeline);
            if (request.getStageId() != null) {
                PipelineStage newStage = pipelineStageRepository.findByIdAndPipeline(request.getStageId(), newPipeline)
                        .orElseThrow(() -> new ResourceNotFoundException("Stage not found with id: " + request.getStageId() + " in new pipeline"));
                deal.setStage(newStage);
                deal.setStatus(resolveDealStatus(request.getStatus(), newStage));
            } else {
                PipelineStage firstStage = newPipeline.getStages().get(0);
                deal.setStage(firstStage);
                deal.setStatus(resolveDealStatus(request.getStatus(), firstStage));
            }
        } else if (request.getStageId() != null && !request.getStageId().equals(deal.getStage().getId())) {
            PipelineStage newStage = pipelineStageRepository.findByIdAndPipeline(request.getStageId(), deal.getPipeline())
                    .orElseThrow(() -> new ResourceNotFoundException("Stage not found with id: " + request.getStageId() + " in pipeline"));
            deal.setStage(newStage);
            deal.setStatus(resolveDealStatus(request.getStatus(), newStage));
        }

        if (request.getStatus() != null) {
            deal.setStatus(request.getStatus());
            if (request.getStatus() == DealStatus.WON || request.getStatus() == DealStatus.LOST) {
                if (deal.getActualCloseDate() == null) {
                    deal.setActualCloseDate(LocalDateTime.now());
                }
            } else {
                deal.setActualCloseDate(null);
            }
        }

        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findByIdAndOrganizationAndDeletedFalse(request.getCustomerId(), deal.getOrganization())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
            deal.setCustomer(customer);
        }

        if (request.getContactId() != null) {
            Contact contact;
            if (deal.getCustomer() != null) {
                contact = contactRepository.findByIdAndCustomer(request.getContactId(), deal.getCustomer())
                        .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + request.getContactId() + " for customer"));
            } else {
                contact = contactRepository.findById(request.getContactId())
                        .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + request.getContactId()));
            }
            deal.setContact(contact);
        }

        if (request.getCustomFields() != null) {
            deal.setCustomFields(request.getCustomFields());
        }

        Deal updated = dealRepository.save(deal);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public DealResponse updateDealStage(UUID dealId, DealStageUpdateRequest request, String userEmail) {
        Deal deal = getDeal(dealId, userEmail);
        workspaceSecurityService.validateCanModify(deal.getOrganization().getId(), userEmail);

        PipelineStage targetStage = pipelineStageRepository.findByIdAndPipeline(request.getStageId(), deal.getPipeline())
                .orElseThrow(() -> new ResourceNotFoundException("Stage not found with id: " + request.getStageId() + " in pipeline: " + deal.getPipeline().getId()));

        deal.setStage(targetStage);

        if (targetStage.getCategory() == StageCategory.WON) {
            deal.setStatus(DealStatus.WON);
            deal.setActualCloseDate(LocalDateTime.now());
        } else if (targetStage.getCategory() == StageCategory.LOST) {
            deal.setStatus(DealStatus.LOST);
            deal.setActualCloseDate(LocalDateTime.now());
        } else {
            deal.setStatus(DealStatus.OPEN);
            deal.setActualCloseDate(null);
        }

        Deal saved = dealRepository.save(deal);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteDeal(UUID dealId, String userEmail) {
        Deal deal = getDeal(dealId, userEmail);
        workspaceSecurityService.validateCanDelete(deal.getOrganization().getId(), userEmail);

        deal.setDeleted(true);
        deal.setDeletedAt(LocalDateTime.now());
        dealRepository.save(deal);
    }

    private DealStatus resolveDealStatus(DealStatus requestedStatus, PipelineStage stage) {
        if (requestedStatus != null) {
            return requestedStatus;
        }
        if (stage.getCategory() == StageCategory.WON) {
            return DealStatus.WON;
        }
        if (stage.getCategory() == StageCategory.LOST) {
            return DealStatus.LOST;
        }
        return DealStatus.OPEN;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private DealResponse mapToResponse(Deal deal) {
        return DealResponse.builder()
                .id(deal.getId())
                .title(deal.getTitle())
                .value(deal.getValue())
                .currency(deal.getCurrency())
                .status(deal.getStatus())
                .expectedCloseDate(deal.getExpectedCloseDate())
                .actualCloseDate(deal.getActualCloseDate())
                .pipelineId(deal.getPipeline() != null ? deal.getPipeline().getId() : null)
                .pipelineName(deal.getPipeline() != null ? deal.getPipeline().getName() : null)
                .stageId(deal.getStage() != null ? deal.getStage().getId() : null)
                .stageName(deal.getStage() != null ? deal.getStage().getName() : null)
                .stageProbability(deal.getStage() != null ? deal.getStage().getProbability() : 0)
                .stageCategory(deal.getStage() != null ? deal.getStage().getCategory() : StageCategory.OPEN)
                .organizationId(deal.getOrganization() != null ? deal.getOrganization().getId() : null)
                .organizationName(deal.getOrganization() != null ? deal.getOrganization().getName() : null)
                .customerId(deal.getCustomer() != null ? deal.getCustomer().getId() : null)
                .customerName(deal.getCustomer() != null ? deal.getCustomer().getFirstName() + " " + deal.getCustomer().getLastName() : null)
                .customerEmail(deal.getCustomer() != null ? deal.getCustomer().getEmail() : null)
                .contactId(deal.getContact() != null ? deal.getContact().getId() : null)
                .contactName(deal.getContact() != null ? deal.getContact().getFirstName() + " " + deal.getContact().getLastName() : null)
                .ownerId(deal.getOwner() != null ? deal.getOwner().getId() : null)
                .ownerName(deal.getOwner() != null ? deal.getOwner().getName() : null)
                .ownerEmail(deal.getOwner() != null ? deal.getOwner().getEmail() : null)
                .customFields(deal.getCustomFields())
                .createdAt(deal.getCreatedAt())
                .updatedAt(deal.getUpdatedAt())
                .build();
    }
}
