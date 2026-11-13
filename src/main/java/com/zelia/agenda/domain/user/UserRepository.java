package com.zelia.agenda.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByToken(String token);

    List<User> findByActiveTrue();

    long countByUserTypeAndActiveTrue(UserType userType);
}
