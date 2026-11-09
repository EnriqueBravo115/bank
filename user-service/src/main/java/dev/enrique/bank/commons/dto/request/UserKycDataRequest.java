package dev.enrique.bank.commons.dto.request;

import dev.enrique.bank.commons.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserKycDataRequest {
    private static final String STATES =
            "AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE";
    static final String CURP_PATTERN = "^[A-Z][AEIOUX][A-Z]{2}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])[HMX]("
            + STATES + ")[B-DF-HJ-NP-TV-Z]{3}[A-Z\\d]\\d$";
    static final String RFC_PATTERN = "^[A-ZÑ&]{4}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])[A-Z\\d]{2}[A\\d]$";

    // 4 letters (2nd a vowel or X), birth date YYMMDD, sex, state code, 3 internal consonants,
    // homonym character and check digit. Uppercase only.
    @NotBlank
    @Pattern(regexp = CURP_PATTERN, message = "must be a valid CURP (18 characters, uppercase)")
    private String curp;

    // Individual (persona física): 4 letters, birth date YYMMDD, 2-character homoclave and check digit (0-9 or A).
    @NotBlank
    @Pattern(regexp = RFC_PATTERN, message = "must be a valid RFC for an individual (13 characters, uppercase)")
    private String rfc;

    private DocumentType documentType;
}
