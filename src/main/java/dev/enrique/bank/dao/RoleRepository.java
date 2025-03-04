package dev.enrique.bank.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.enrique.bank.model.Role;
import dev.enrique.bank.commons.enums.RoleName;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Role findByRoleName(RoleName roleName);
}
