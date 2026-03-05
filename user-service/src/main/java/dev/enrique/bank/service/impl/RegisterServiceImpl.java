package dev.enrique.bank.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.commons.exception.InvalidRegistrationStepException;
import dev.enrique.bank.commons.exception.UserNotFoundException;
import dev.enrique.bank.dao.UserFinancialInfoRepository;
import dev.enrique.bank.dao.UserKycRepository;
import dev.enrique.bank.dao.UserProfileRepository;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dto.request.UserFinancialInfoRequest;
import dev.enrique.bank.dto.request.UserKycDataRequest;
import dev.enrique.bank.dto.request.UserProfileRequest;
import dev.enrique.bank.dto.request.UserRegisterRequest;
import dev.enrique.bank.dto.response.UserRegisterResponse;
import dev.enrique.bank.model.User;
import dev.enrique.bank.model.UserFinancialInfo;
import dev.enrique.bank.model.UserKyc;
import dev.enrique.bank.model.UserProfile;
import dev.enrique.bank.service.KeycloakUserService;
import dev.enrique.bank.service.RegisterService;
import dev.enrique.bank.service.util.BasicMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserKycRepository userKycRepository;
    private final UserFinancialInfoRepository userFinancialInfoRepository;
    private final KeycloakUserService keycloakUserService;
    private final BasicMapper basicMapper;

    @Override
    public UserRegisterResponse register(UserRegisterRequest request) {
        String keycloakId = keycloakUserService.createUser(request);

        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setEmail(request.getEmail());
        user.setPhoneCode(request.getPhoneCode());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(UserRole.CUSTOMER_BASIC);
        user.setRegisterStatus(RegisterStatus.REGISTER);
        user.setActive(false);

        userRepository.save(user);

        return basicMapper.convertToResponse(user, UserRegisterResponse.class);
    }

    @Override
    public void updateProfile(Long userId, UserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        validateStep(user, RegisterStatus.REGISTER);

        UserProfile userProfile = new UserProfile();
        userProfile.setUser(user);
        userProfile.setNames(request.getNames());
        userProfile.setFirstSurname(request.getFirstSurname());
        userProfile.setSecondSurname(request.getSecondSurname());
        userProfile.setGender(request.getGender());
        userProfile.setBirthday(request.getBirthday());
        userProfile.setCountryOfBirth(request.getCountryOfBirth());

        userProfileRepository.save(userProfile);
        user.setRegisterStatus(RegisterStatus.PROFILE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void updateKycData(Long userId, UserKycDataRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        validateStep(user, RegisterStatus.PROFILE);

        UserKyc kyc = new UserKyc();
        kyc.setUser(user);
        kyc.setCurp(request.getCurp());
        kyc.setRfc(request.getRfc());
        kyc.setDocumentType(request.getDocumentType());

        userKycRepository.save(kyc);

        user.setRegisterStatus(RegisterStatus.KYC);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void updateFinancialInfo(Long userId, UserFinancialInfoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        validateStep(user, RegisterStatus.KYC);

        UserFinancialInfo info = new UserFinancialInfo();
        info.setUser(user);
        info.setOccupationType(request.getOccupationType());
        info.setEmployerName(request.getEmployerName());
        info.setIncomeSource(request.getIncomeSource());
        info.setMonthlyIncome(request.getMonthlyIncome());
        info.setMaritalStatus(request.getMaritalStatus());

        userFinancialInfoRepository.save(info);

        user.setRegisterStatus(RegisterStatus.COMPLETE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private void validateStep(User user, RegisterStatus expectedStatus) {
        if (user.getRegisterStatus() != expectedStatus) {
            throw new InvalidRegistrationStepException(
                    expectedStatus,
                    user.getRegisterStatus());
        }
    }
}
