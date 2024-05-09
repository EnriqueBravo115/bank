package dev.enrique.bank.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.enrique.bank.domain.Role;
import dev.enrique.bank.commons.enums.RoleName;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Role findByRoleName(RoleName roleName);
}
