package com.adrplatform.auth.repository;

import com.adrplatform.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Query("select u from User u join fetch u.workspace where u.id = :id")
    Optional<User> findByIdWithWorkspace(@Param("id") UUID id);

    Optional<User> findByIdAndWorkspace_Id(UUID id, UUID workspaceId);

    List<User> findAllByWorkspace_Id(UUID workspaceId);

    long countByWorkspace_Id(UUID workspaceId);
}
