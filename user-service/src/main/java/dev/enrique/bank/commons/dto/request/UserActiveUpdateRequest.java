package dev.enrique.bank.commons.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserActiveUpdateRequest {
    @NotNull
    private Boolean active;
}
