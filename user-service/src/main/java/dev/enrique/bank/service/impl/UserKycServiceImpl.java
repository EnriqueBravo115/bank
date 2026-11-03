package dev.enrique.bank.service.impl;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.response.UserKycDetailResponse;
import dev.enrique.bank.commons.dto.response.UserKycResponse;
import dev.enrique.bank.commons.exception.UserNotFoundException;
import dev.enrique.bank.dao.UserKycRepository;
import dev.enrique.bank.service.UserKycService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserKycServiceImpl implements UserKycService {
    private final UserKycRepository userKycRepository;

    @Override
    public UserKycResponse getKycByKeycloakId(String keycloakId) {
        return userKycRepository.getKycByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException("keycloakId", keycloakId));
    }

    @Override
    public UserKycDetailResponse getKycDetailByUserId(Long userId) {
        return userKycRepository.getKycDetailByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public UserKycResponse getKycByRfc(String rfc) {
        return userKycRepository.getKycByRfc(rfc)
                .orElseThrow(() -> new UserNotFoundException("rfc", rfc));
    }

    @Override
    public UserKycResponse getKycByCurp(String curp) {
        return userKycRepository.getKycByCurp(curp)
                .orElseThrow(() -> new UserNotFoundException("curp", curp));
    }
}
