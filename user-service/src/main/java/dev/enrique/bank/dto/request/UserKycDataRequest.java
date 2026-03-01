package dev.enrique.bank.dto.request;

import dev.enrique.bank.commons.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserKycDataRequest {
    @NotBlank
    @Size(min = 18, max = 18)
    private String curp;

    @NotBlank
    @Size(min = 13, max = 13)
    private String rfc;

    private DocumentType documentType;
}
