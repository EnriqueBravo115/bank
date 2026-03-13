package dev.enrique.bank.commons.dto.response;

import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.commons.enums.UserRole;

public record UserDetailedResponse(
        Long id,
        String email,
        boolean active,
        UserRole role,
        RegisterStatus registerStatus) {
}
