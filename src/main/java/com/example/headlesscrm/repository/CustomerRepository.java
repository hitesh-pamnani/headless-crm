package com.example.headlesscrm.repository;

import com.example.headlesscrm.domain.Customer;
import com.example.headlesscrm.domain.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Page<Customer> findAllByOrganizationAndDeletedFalse(Organization organization, Pageable pageable);

    Optional<Customer> findByIdAndOrganizationAndDeletedFalse(UUID id, Organization organization);

    boolean existsByEmailAndOrganizationAndDeletedFalse(String email, Organization organization);

    boolean existsByEmailAndOrganizationAndDeletedFalseAndIdNot(String email, Organization organization, UUID id);
}
