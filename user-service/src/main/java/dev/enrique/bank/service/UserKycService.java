package dev.enrique.bank.service;

import java.util.List;

import dev.enrique.bank.commons.dto.response.UserKycDetailResponse;
import dev.enrique.bank.commons.dto.response.UserKycResponse;
import dev.enrique.bank.commons.enums.DocumentType;
import dev.enrique.bank.commons.enums.RegisterStatus;

public interface UserKycService {
    UserKycResponse getKycByUserId(Long userId);

    UserKycResponse getKycByRfc(String rfc);

    UserKycResponse getKycByCurp(String curp);

    UserKycDetailResponse getKycDetailByUserId(Long userId);

    UserKycResponse getKycByEmail(String email);

    List<UserKycResponse> getKycByDocumentType(DocumentType documentType);

    List<UserKycDetailResponse> getAllKycDetailed();

    List<UserKycResponse> getKycFromActiveUsers();

    List<UserKycDetailResponse> getKycByRegisterStatus(RegisterStatus status);

    boolean existsKycByUserId(Long userId);
}
