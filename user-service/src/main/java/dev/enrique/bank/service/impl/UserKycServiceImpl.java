package dev.enrique.bank.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.response.UserKycDetailResponse;
import dev.enrique.bank.commons.dto.response.UserKycResponse;
import dev.enrique.bank.commons.enums.DocumentType;
import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.commons.exception.UserNotFoundException;
import dev.enrique.bank.dao.UserKycRepository;
import dev.enrique.bank.service.UserKycService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserKycServiceImpl implements UserKycService {
    private final UserKycRepository userKycRepository;

    @Override
    public UserKycResponse getKycByUserId(Long userId) {
        return userKycRepository.getKycByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public UserKycResponse getKycByRfc(String rfc) {
        return userKycRepository.getKycByRfc(rfc)
                .orElseThrow(() -> new UserNotFoundException(rfc));
    }

    @Override
    public UserKycResponse getKycByCurp(String curp) {
        return userKycRepository.getKycByCurp(curp)
                .orElseThrow(() -> new UserNotFoundException(curp));
    }

    @Override
    public UserKycDetailResponse getKycDetailByUserId(Long userId) {
        return userKycRepository.getKycDetailByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public UserKycResponse getKycByEmail(String email) {
        return userKycRepository.getKycByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Override
    public List<UserKycResponse> getKycByDocumentType(DocumentType documentType) {
        return userKycRepository.getKycByDocumentType(documentType);
    }

    @Override
    public List<UserKycDetailResponse> getAllKycDetailed() {
        return userKycRepository.getAllKycDetailed();
    }

    @Override
    public List<UserKycResponse> getKycFromActiveUsers() {
        return userKycRepository.getKycFromActiveUsers();
    }

    @Override
    public List<UserKycDetailResponse> getKycByRegisterStatus(RegisterStatus status) {
        return userKycRepository.getKycByRegisterStatus(status);
    }

    @Override
    public boolean existsKycByUserId(Long userId) {
        return userKycRepository.existsKycByUserId(userId);
    }
}
