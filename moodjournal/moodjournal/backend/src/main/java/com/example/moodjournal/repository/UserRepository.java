package com.example.moodjournal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.moodjournal.model.User;

public interface UserRepository extends JpaRepository<User, java.util.UUID> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}
