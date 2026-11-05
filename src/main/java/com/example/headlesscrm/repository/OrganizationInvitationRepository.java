package com.example.headlesscrm.repository;

import com.example.headlesscrm.domain.InvitationStatus;
import com.example.headlesscrm.domain.Organization;
import com.example.headlesscrm.domain.OrganizationInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationInvitationRepository extends JpaRepository<OrganizationInvitation, UUID> {
    List<OrganizationInvitation> findAllByOrganizationAndStatus(Organization organization, InvitationStatus status);

    List<OrganizationInvitation> findAllByEmailIgnoreCaseAndStatus(String email, InvitationStatus status);

    Optional<OrganizationInvitation> findByIdAndOrganization(UUID id, Organization organization);

    boolean existsByOrganizationAndEmailIgnoreCaseAndStatus(Organization organization, String email, InvitationStatus status);
}
