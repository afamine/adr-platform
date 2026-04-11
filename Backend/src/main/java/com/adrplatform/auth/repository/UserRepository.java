package com.adrplatform.auth.repository;

import com.adrplatform.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndWorkspace_Id(UUID id, UUID workspaceId);

    List<User> findAllByWorkspace_Id(UUID workspaceId);
}
