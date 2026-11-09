package com.example.headlesscrm.service.impl;

import com.example.headlesscrm.domain.*;
import com.example.headlesscrm.dto.ActivityRequest;
import com.example.headlesscrm.dto.ActivityResponse;
import com.example.headlesscrm.exception.BadRequestException;
import com.example.headlesscrm.exception.ResourceNotFoundException;
import com.example.headlesscrm.repository.*;
import com.example.headlesscrm.security.WorkspaceSecurityService;
import com.example.headlesscrm.service.ActivityService;
import com.example.headlesscrm.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final DealRepository dealRepository;
    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;
    private final WorkspaceSecurityService workspaceSecurityService;

    @Override
    @Transactional
    public ActivityResponse createActivity(UUID workspaceId, ActivityRequest request, String userEmail) {
        Organization organization = workspaceService.resolveWorkspace(workspaceId, userEmail);
        workspaceSecurityService.validateCanModify(organization.getId(), userEmail);
        User user = findUserByEmail(userEmail);

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

        Deal deal = null;
        if (request.getDealId() != null) {
            deal = dealRepository.findByIdAndOrganizationAndDeletedFalse(request.getDealId(), organization)
                    .orElseThrow(() -> new ResourceNotFoundException("Deal not found with id: " + request.getDealId()));
        }

        ActivityStatus status = resolveStatus(request.getStatus(), request.getType());
        LocalDateTime completedAt = (status == ActivityStatus.COMPLETED) ? LocalDateTime.now() : null;

        Activity activity = Activity.builder()
                .type(request.getType())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .status(status)
                .dueDate(request.getDueDate())
                .completedAt(completedAt)
                .durationMinutes(request.getDurationMinutes())
                .organization(organization)
                .customer(customer)
                .contact(contact)
                .deal(deal)
                .user(user)
                .customFields(request.getCustomFields() != null ? request.getCustomFields() : new HashMap<>())
                .build();

        Activity saved = activityRepository.save(activity);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityResponse> getActivities(UUID workspaceId, ActivityType type, ActivityStatus status, UUID customerId, UUID contactId, UUID dealId, String userEmail, Pageable pageable) {
        Organization organization = workspaceService.resolveWorkspace(workspaceId, userEmail);
        workspaceSecurityService.getMembershipOrThrow(organization.getId(), userEmail);

        return activityRepository.findFiltered(organization, type, status, customerId, contactId, dealId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityResponse getActivityById(UUID activityId, String userEmail) {
        Activity activity = findActivityOrThrow(activityId);
        workspaceSecurityService.getMembershipOrThrow(activity.getOrganization().getId(), userEmail);
        return mapToResponse(activity);
    }

    @Override
    @Transactional
    public ActivityResponse updateActivity(UUID activityId, ActivityRequest request, String userEmail) {
        Activity activity = findActivityOrThrow(activityId);
        workspaceSecurityService.validateCanModify(activity.getOrganization().getId(), userEmail);

        activity.setType(request.getType());
        activity.setTitle(request.getTitle().trim());
        activity.setDescription(request.getDescription());
        activity.setDueDate(request.getDueDate());
        activity.setDurationMinutes(request.getDurationMinutes());

        if (request.getStatus() != null) {
            activity.setStatus(request.getStatus());
            if (request.getStatus() == ActivityStatus.COMPLETED && activity.getCompletedAt() == null) {
                activity.setCompletedAt(LocalDateTime.now());
            } else if (request.getStatus() != ActivityStatus.COMPLETED) {
                activity.setCompletedAt(null);
            }
        }

        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findByIdAndOrganizationAndDeletedFalse(request.getCustomerId(), activity.getOrganization())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
            activity.setCustomer(customer);
        }

        if (request.getContactId() != null) {
            Contact contact;
            if (activity.getCustomer() != null) {
                contact = contactRepository.findByIdAndCustomer(request.getContactId(), activity.getCustomer())
                        .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + request.getContactId() + " for customer"));
            } else {
                contact = contactRepository.findById(request.getContactId())
                        .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + request.getContactId()));
            }
            activity.setContact(contact);
        }

        if (request.getDealId() != null) {
            Deal deal = dealRepository.findByIdAndOrganizationAndDeletedFalse(request.getDealId(), activity.getOrganization())
                    .orElseThrow(() -> new ResourceNotFoundException("Deal not found with id: " + request.getDealId()));
            activity.setDeal(deal);
        }

        if (request.getCustomFields() != null) {
            activity.setCustomFields(request.getCustomFields());
        }

        Activity updated = activityRepository.save(activity);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ActivityResponse completeActivity(UUID activityId, String userEmail) {
        Activity activity = findActivityOrThrow(activityId);
        workspaceSecurityService.validateCanModify(activity.getOrganization().getId(), userEmail);

        activity.setStatus(ActivityStatus.COMPLETED);
        activity.setCompletedAt(LocalDateTime.now());

        Activity saved = activityRepository.save(activity);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteActivity(UUID activityId, String userEmail) {
        Activity activity = findActivityOrThrow(activityId);
        workspaceSecurityService.validateCanDelete(activity.getOrganization().getId(), userEmail);
        activityRepository.delete(activity);
    }

    private Activity findActivityOrThrow(UUID activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with id: " + activityId));
    }

    private ActivityStatus resolveStatus(ActivityStatus requestedStatus, ActivityType type) {
        if (requestedStatus != null) {
            return requestedStatus;
        }
        return type == ActivityType.NOTE ? ActivityStatus.COMPLETED : ActivityStatus.PENDING;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private ActivityResponse mapToResponse(Activity activity) {
        return ActivityResponse.builder()
                .id(activity.getId())
                .type(activity.getType())
                .title(activity.getTitle())
                .description(activity.getDescription())
                .status(activity.getStatus())
                .dueDate(activity.getDueDate())
                .completedAt(activity.getCompletedAt())
                .durationMinutes(activity.getDurationMinutes())
                .organizationId(activity.getOrganization() != null ? activity.getOrganization().getId() : null)
                .customerId(activity.getCustomer() != null ? activity.getCustomer().getId() : null)
                .customerName(activity.getCustomer() != null ? activity.getCustomer().getFirstName() + " " + activity.getCustomer().getLastName() : null)
                .contactId(activity.getContact() != null ? activity.getContact().getId() : null)
                .contactName(activity.getContact() != null ? activity.getContact().getFirstName() + " " + activity.getContact().getLastName() : null)
                .dealId(activity.getDeal() != null ? activity.getDeal().getId() : null)
                .dealTitle(activity.getDeal() != null ? activity.getDeal().getTitle() : null)
                .userId(activity.getUser() != null ? activity.getUser().getId() : null)
                .userName(activity.getUser() != null ? activity.getUser().getName() : null)
                .userEmail(activity.getUser() != null ? activity.getUser().getEmail() : null)
                .customFields(activity.getCustomFields())
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }
}
