package com.example.headlesscrm.repository;

import com.example.headlesscrm.domain.Organization;
import com.example.headlesscrm.domain.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PipelineRepository extends JpaRepository<Pipeline, UUID> {
    Optional<Pipeline> findByIdAndOrganization(UUID id, Organization organization);
    List<Pipeline> findAllByOrganization(Organization organization);
    Optional<Pipeline> findByOrganizationAndIsDefaultTrue(Organization organization);
    boolean existsByNameAndOrganization(String name, Organization organization);
    boolean existsByNameAndOrganizationAndIdNot(String name, Organization organization, UUID id);
}
