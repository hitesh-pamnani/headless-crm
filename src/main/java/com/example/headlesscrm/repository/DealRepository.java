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
public interface DealRepository extends JpaRepository<Deal, UUID> {
    Optional<Deal> findByIdAndDeletedFalse(UUID id);
    Optional<Deal> findByIdAndOrganizationAndDeletedFalse(UUID id, Organization organization);
    Page<Deal> findAllByOrganizationAndDeletedFalse(Organization organization, Pageable pageable);
    List<Deal> findAllByCustomerAndDeletedFalseOrderByCreatedAtDesc(Customer customer);
    boolean existsByPipelineAndDeletedFalse(Pipeline pipeline);
    boolean existsByStageAndDeletedFalse(PipelineStage stage);

    @Query("SELECT d FROM Deal d WHERE d.organization = :org AND d.deleted = false " +
           "AND (:pipelineId IS NULL OR d.pipeline.id = :pipelineId) " +
           "AND (:stageId IS NULL OR d.stage.id = :stageId) " +
           "AND (:customerId IS NULL OR d.customer.id = :customerId) " +
           "AND (:status IS NULL OR d.status = :status)")
    Page<Deal> findFiltered(
            @Param("org") Organization org,
            @Param("pipelineId") UUID pipelineId,
            @Param("stageId") UUID stageId,
            @Param("customerId") UUID customerId,
            @Param("status") DealStatus status,
            Pageable pageable
    );
}
