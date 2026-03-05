package dev.enrique.bank.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.enrique.bank.model.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

}
