package com.example.headlesscrm.repository;

import com.example.headlesscrm.domain.Organization;
import com.example.headlesscrm.domain.OrganizationMember;
import com.example.headlesscrm.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {
    List<OrganizationMember> findAllByUser(User user);

    List<OrganizationMember> findAllByOrganization(Organization organization);

    Optional<OrganizationMember> findByOrganizationAndUser(Organization organization, User user);

    @Query("SELECT om FROM OrganizationMember om WHERE om.organization.id = :orgId AND om.user.email = :email")
    Optional<OrganizationMember> findByOrganizationIdAndUserEmail(@Param("orgId") UUID orgId, @Param("email") String email);

    boolean existsByOrganizationAndUser(Organization organization, User user);

    long countByOrganization(Organization organization);

    Optional<OrganizationMember> findFirstByUserOrderByJoinedAtAsc(User user);
}
