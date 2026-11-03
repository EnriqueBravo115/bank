package dev.enrique.bank.service;

import dev.enrique.bank.commons.dto.response.UserKycDetailResponse;
import dev.enrique.bank.commons.dto.response.UserKycResponse;

public interface UserKycService {
    UserKycResponse getKycByKeycloakId(String keycloakId);

    UserKycDetailResponse getKycDetailByUserId(Long userId);

    UserKycResponse getKycByRfc(String rfc);

    UserKycResponse getKycByCurp(String curp);
}
