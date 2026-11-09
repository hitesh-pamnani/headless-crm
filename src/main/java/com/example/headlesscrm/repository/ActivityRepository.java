package com.example.headlesscrm.repository;

import com.example.headlesscrm.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    Optional<Activity> findByIdAndOrganization(UUID id, Organization organization);
    List<Activity> findAllByCustomerOrderByCreatedAtDesc(Customer customer);
    List<Activity> findAllByDealOrderByCreatedAtDesc(Deal deal);
    List<Activity> findAllByOrganizationOrderByCreatedAtDesc(Organization organization);

    @Query("SELECT a FROM Activity a WHERE a.organization = :org " +
           "AND (:type IS NULL OR a.type = :type) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:customerId IS NULL OR a.customer.id = :customerId) " +
           "AND (:contactId IS NULL OR a.contact.id = :contactId) " +
           "AND (:dealId IS NULL OR a.deal.id = :dealId)")
    Page<Activity> findFiltered(
            @Param("org") Organization org,
            @Param("type") ActivityType type,
            @Param("status") ActivityStatus status,
            @Param("customerId") UUID customerId,
            @Param("contactId") UUID contactId,
            @Param("dealId") UUID dealId,
            Pageable pageable
    );
}
