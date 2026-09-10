package com.giglister.repository;

import com.giglister.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    Optional<User> findByVerificationToken(String token);

    Optional<User> findByResetToken(String token);

    List<User> findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(String email, String username);
}
