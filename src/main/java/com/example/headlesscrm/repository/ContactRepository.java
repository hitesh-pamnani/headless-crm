package com.example.headlesscrm.repository;

import com.example.headlesscrm.domain.Contact;
import com.example.headlesscrm.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<Contact, UUID> {
    Page<Contact> findAllByCustomer(Customer customer, Pageable pageable);
    Optional<Contact> findByIdAndCustomer(UUID id, Customer customer);
    boolean existsByEmailAndCustomer(String email, Customer customer);
}
