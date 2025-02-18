package com.example.headlesscrm.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.headlesscrm.domain.Lead;
import com.example.headlesscrm.domain.User;

public interface LeadRepository extends JpaRepository<Lead, UUID> {
    Page<Lead> findAllByOwner(User owner, Pageable pageable);

    Optional<Lead> findByIdAndOwner(UUID id, User owner);

    boolean existsByEmailAndOwner(String email, User owner);
}
