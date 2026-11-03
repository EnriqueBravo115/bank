package dev.enrique.bank.commons.dto.request;

import dev.enrique.bank.commons.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserRoleUpdateRequest {
    @NotNull
    private UserRole role;
}
