package com.example.headlesscrm.service.impl;

import com.example.headlesscrm.domain.*;
import com.example.headlesscrm.dto.TimelineItemResponse;
import com.example.headlesscrm.exception.ResourceNotFoundException;
import com.example.headlesscrm.repository.*;
import com.example.headlesscrm.security.WorkspaceSecurityService;
import com.example.headlesscrm.service.CustomerService;
import com.example.headlesscrm.service.TimelineService;
import com.example.headlesscrm.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TimelineServiceImpl implements TimelineService {

    private final CustomerService customerService;
    private final ContactRepository contactRepository;
    private final DealRepository dealRepository;
    private final ActivityRepository activityRepository;
    private final WorkspaceService workspaceService;
    private final WorkspaceSecurityService workspaceSecurityService;

    @Override
    @Transactional(readOnly = true)
    public Page<TimelineItemResponse> getCustomerTimeline(UUID customerId, String userEmail, Pageable pageable) {
        Customer customer = customerService.getCustomer(customerId, userEmail);

        List<TimelineItemResponse> items = new ArrayList<>();

        // Customer created milestone
        if (customer.getCreatedAt() != null) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("email", customer.getEmail());
            meta.put("phone", customer.getPhone());
            if (customer.getCustomFields() != null) {
                meta.put("customFields", customer.getCustomFields());
            }

            items.add(TimelineItemResponse.builder()
                    .id("customer-created-" + customer.getId())
                    .timestamp(customer.getCreatedAt())
                    .type("CUSTOMER_CREATED")
                    .category("MILESTONE")
                    .title("Customer profile created: " + customer.getFirstName() + " " + customer.getLastName())
                    .description("Added to workspace " + customer.getOrganization().getName())
                    .actorName(customer.getOwner() != null ? customer.getOwner().getName() : null)
                    .actorEmail(customer.getOwner() != null ? customer.getOwner().getEmail() : null)
                    .entityType("CUSTOMER")
                    .entityId(customer.getId())
                    .metadata(meta)
                    .build());
        }

        // Contacts added under this customer
        List<Contact> contacts = contactRepository.findAllByCustomer(customer, Pageable.unpaged()).getContent();
        for (Contact contact : contacts) {
            if (contact.getCreatedAt() != null) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("email", contact.getEmail());
                meta.put("phone", contact.getPhone());

                items.add(TimelineItemResponse.builder()
                        .id("contact-created-" + contact.getId())
                        .timestamp(contact.getCreatedAt())
                        .type("CONTACT_CREATED")
                        .category("MILESTONE")
                        .title("Contact added: " + contact.getFirstName() + " " + contact.getLastName())
                        .description("Direct contact for " + customer.getFirstName() + " " + customer.getLastName())
                        .entityType("CONTACT")
                        .entityId(contact.getId())
                        .metadata(meta)
                        .build());
            }
        }

        // Deals linked to this customer
        List<Deal> deals = dealRepository.findAllByCustomerAndDeletedFalseOrderByCreatedAtDesc(customer);
        for (Deal deal : deals) {
            if (deal.getCreatedAt() != null) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("value", deal.getValue());
                meta.put("currency", deal.getCurrency());
                meta.put("stageName", deal.getStage() != null ? deal.getStage().getName() : null);
                meta.put("status", deal.getStatus().name());

                items.add(TimelineItemResponse.builder()
                        .id("deal-created-" + deal.getId())
                        .timestamp(deal.getCreatedAt())
                        .type("DEAL_CREATED")
                        .category("MILESTONE")
                        .title("Deal opened: " + deal.getTitle() + " (" + deal.getCurrency() + " " + deal.getValue() + ")")
                        .description("Pipeline: " + (deal.getPipeline() != null ? deal.getPipeline().getName() : "N/A"))
                        .actorName(deal.getOwner() != null ? deal.getOwner().getName() : null)
                        .actorEmail(deal.getOwner() != null ? deal.getOwner().getEmail() : null)
                        .entityType("DEAL")
                        .entityId(deal.getId())
                        .metadata(meta)
                        .build());
            }

            if (deal.getActualCloseDate() != null) {
                Map<String, Object> closeMeta = new HashMap<>();
                closeMeta.put("value", deal.getValue());
                closeMeta.put("currency", deal.getCurrency());
                closeMeta.put("status", deal.getStatus().name());

                items.add(TimelineItemResponse.builder()
                        .id("deal-closed-" + deal.getId())
                        .timestamp(deal.getActualCloseDate())
                        .type(deal.getStatus() == DealStatus.WON ? "DEAL_WON" : "DEAL_LOST")
                        .category("MILESTONE")
                        .title("Deal concluded as " + deal.getStatus() + ": " + deal.getTitle())
                        .description("Final value: " + deal.getCurrency() + " " + deal.getValue())
                        .entityType("DEAL")
                        .entityId(deal.getId())
                        .metadata(closeMeta)
                        .build());
            }
        }

        // Activities linked to this customer
        List<Activity> activities = activityRepository.findAllByCustomerOrderByCreatedAtDesc(customer);
        for (Activity activity : activities) {
            items.add(mapActivityToTimelineItem(activity));
        }

        return paginateList(items, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TimelineItemResponse> getDealTimeline(UUID dealId, String userEmail, Pageable pageable) {
        Deal deal = dealRepository.findByIdAndDeletedFalse(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found with id: " + dealId));

        workspaceSecurityService.getMembershipOrThrow(deal.getOrganization().getId(), userEmail);

        List<TimelineItemResponse> items = new ArrayList<>();

        if (deal.getCreatedAt() != null) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("value", deal.getValue());
            meta.put("currency", deal.getCurrency());
            meta.put("stageName", deal.getStage() != null ? deal.getStage().getName() : null);
            meta.put("status", deal.getStatus().name());

            items.add(TimelineItemResponse.builder()
                    .id("deal-created-" + deal.getId())
                    .timestamp(deal.getCreatedAt())
                    .type("DEAL_CREATED")
                    .category("MILESTONE")
                    .title("Deal opened: " + deal.getTitle() + " (" + deal.getCurrency() + " " + deal.getValue() + ")")
                    .description("Pipeline: " + (deal.getPipeline() != null ? deal.getPipeline().getName() : "N/A"))
                    .actorName(deal.getOwner() != null ? deal.getOwner().getName() : null)
                    .actorEmail(deal.getOwner() != null ? deal.getOwner().getEmail() : null)
                    .entityType("DEAL")
                    .entityId(deal.getId())
                    .metadata(meta)
                    .build());
        }

        if (deal.getActualCloseDate() != null) {
            Map<String, Object> closeMeta = new HashMap<>();
            closeMeta.put("value", deal.getValue());
            closeMeta.put("currency", deal.getCurrency());
            closeMeta.put("status", deal.getStatus().name());

            items.add(TimelineItemResponse.builder()
                    .id("deal-closed-" + deal.getId())
                    .timestamp(deal.getActualCloseDate())
                    .type(deal.getStatus() == DealStatus.WON ? "DEAL_WON" : "DEAL_LOST")
                    .category("MILESTONE")
                    .title("Deal concluded as " + deal.getStatus() + ": " + deal.getTitle())
                    .description("Final value: " + deal.getCurrency() + " " + deal.getValue())
                    .entityType("DEAL")
                    .entityId(deal.getId())
                    .metadata(closeMeta)
                    .build());
        }

        List<Activity> activities = activityRepository.findAllByDealOrderByCreatedAtDesc(deal);
        for (Activity activity : activities) {
            items.add(mapActivityToTimelineItem(activity));
        }

        return paginateList(items, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TimelineItemResponse> getWorkspaceTimeline(UUID workspaceId, String userEmail, Pageable pageable) {
        Organization organization = workspaceService.resolveWorkspace(workspaceId, userEmail);
        workspaceSecurityService.getMembershipOrThrow(organization.getId(), userEmail);

        List<TimelineItemResponse> items = new ArrayList<>();

        List<Activity> activities = activityRepository.findAllByOrganizationOrderByCreatedAtDesc(organization);
        for (Activity activity : activities) {
            items.add(mapActivityToTimelineItem(activity));
        }

        List<Deal> deals = dealRepository.findAllByOrganizationAndDeletedFalse(organization, Pageable.unpaged()).getContent();
        for (Deal deal : deals) {
            if (deal.getCreatedAt() != null) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("value", deal.getValue());
                meta.put("currency", deal.getCurrency());
                meta.put("stageName", deal.getStage() != null ? deal.getStage().getName() : null);

                items.add(TimelineItemResponse.builder()
                        .id("deal-created-" + deal.getId())
                        .timestamp(deal.getCreatedAt())
                        .type("DEAL_CREATED")
                        .category("MILESTONE")
                        .title("Deal opened: " + deal.getTitle())
                        .description("Customer: " + (deal.getCustomer() != null ? deal.getCustomer().getFirstName() + " " + deal.getCustomer().getLastName() : "Unassigned"))
                        .actorName(deal.getOwner() != null ? deal.getOwner().getName() : null)
                        .actorEmail(deal.getOwner() != null ? deal.getOwner().getEmail() : null)
                        .entityType("DEAL")
                        .entityId(deal.getId())
                        .metadata(meta)
                        .build());
            }
        }

        return paginateList(items, pageable);
    }

    private TimelineItemResponse mapActivityToTimelineItem(Activity activity) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("status", activity.getStatus().name());
        meta.put("durationMinutes", activity.getDurationMinutes());
        meta.put("dueDate", activity.getDueDate());
        meta.put("completedAt", activity.getCompletedAt());
        if (activity.getCustomFields() != null) {
            meta.put("customFields", activity.getCustomFields());
        }

        return TimelineItemResponse.builder()
                .id("activity-" + activity.getId())
                .timestamp(activity.getCreatedAt())
                .type(activity.getType().name())
                .category("ACTIVITY")
                .title(activity.getTitle())
                .description(activity.getDescription())
                .actorName(activity.getUser() != null ? activity.getUser().getName() : null)
                .actorEmail(activity.getUser() != null ? activity.getUser().getEmail() : null)
                .entityType("ACTIVITY")
                .entityId(activity.getId())
                .metadata(meta)
                .build();
    }

    private Page<TimelineItemResponse> paginateList(List<TimelineItemResponse> items, Pageable pageable) {
        // Sort chronologically descending (newest first)
        items.sort(Comparator.comparing(TimelineItemResponse::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())));

        int start = (int) pageable.getOffset();
        if (start >= items.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, items.size());
        }
        int end = Math.min(start + pageable.getPageSize(), items.size());
        return new PageImpl<>(items.subList(start, end), pageable, items.size());
    }
}
