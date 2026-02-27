package dev.enrique.bank.dto.response;

import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;
import dev.enrique.bank.commons.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterResponse {
    private Long id;
    private String keycloakId;
    private String email;
    private String phoneCode;
    private String phoneNumber;
    private String names;
    private String firstSurname;
    private String secondSurname;
    private Gender gender;
    private String birthday;
    private Country countryOfBirth;
    private UserRole role;
    private boolean active;
    private LocalDateTime registrationDate;
    private LocalDateTime updatedAt;
}
