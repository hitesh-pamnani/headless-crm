package com.example.headlesscrm.repository;

import com.example.headlesscrm.domain.Customer;
import com.example.headlesscrm.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Page<Customer> findAllByOwner(User owner, Pageable pageable);
    Optional<Customer> findByIdAndOwner(UUID id, User owner);
    boolean existsByEmailAndOwner(String email, User owner);
}
