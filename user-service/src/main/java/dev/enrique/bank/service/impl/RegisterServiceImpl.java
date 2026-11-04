package dev.enrique.bank.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.commons.exception.DuplicateUserDataException;
import dev.enrique.bank.commons.exception.InvalidRegistrationStepException;
import dev.enrique.bank.commons.exception.UserNotFoundException;
import dev.enrique.bank.dao.UserFinancialInfoRepository;
import dev.enrique.bank.dao.UserKycRepository;
import dev.enrique.bank.dao.UserProfileRepository;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.broker.outbox.AccountRequestOutbox;
import dev.enrique.bank.commons.dto.request.UserFinancialInfoRequest;
import dev.enrique.bank.commons.dto.request.UserKycDataRequest;
import dev.enrique.bank.commons.dto.request.UserProfileRequest;
import dev.enrique.bank.commons.dto.request.UserRegisterRequest;
import dev.enrique.bank.commons.dto.response.UserRegisterResponse;
import dev.enrique.bank.model.User;
import dev.enrique.bank.model.UserFinancialInfo;
import dev.enrique.bank.model.UserKyc;
import dev.enrique.bank.model.UserProfile;
import dev.enrique.bank.service.KeycloakUserService;
import dev.enrique.bank.service.RegisterService;
import dev.enrique.bank.commons.util.BasicMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserKycRepository userKycRepository;
    private final UserFinancialInfoRepository userFinancialInfoRepository;
    private final KeycloakUserService keycloakUserService;
    private final AccountRequestOutbox accountRequestOutbox;
    private final BasicMapper basicMapper;

    @Override
    @Transactional
    public UserRegisterResponse register(UserRegisterRequest request) {
        // Checked before Keycloak so a duplicate never creates (and then has to delete) a Keycloak user.
        // A concurrent duplicate still hits the DB unique constraint and is compensated below.
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateUserDataException("email");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateUserDataException("phone number");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhoneCode(request.getPhoneCode());
        user.setPhoneNumber(request.getPhoneNumber());
        // Role is granted at the KYC step, together with the Keycloak role.
        user.setRegisterStatus(RegisterStatus.REGISTER);
        user.setActive(true);

        String keycloakId = keycloakUserService.createUser(request);
        user.setKeycloakId(keycloakId);

        try {
            userRepository.saveAndFlush(user);
            keycloakUserService.updateRegistrationStatus(keycloakId, RegisterStatus.REGISTER.name());
        } catch (RuntimeException ex) {
            // Compensate: avoid an orphan Keycloak user that would block re-registration with the same email.
            keycloakUserService.deleteUser(keycloakId);
            throw ex;
        }

        return basicMapper.convertToResponse(user, UserRegisterResponse.class);
    }

    @Override
    public void updateProfile(String keycloakId, UserProfileRequest request) {
        User user = findByKeycloakId(keycloakId);

        validateStep(user, RegisterStatus.REGISTER);

        UserProfile userProfile = userProfileRepository.findById(user.getId())
                .orElse(new UserProfile());

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

        keycloakUserService.updateUser(user.getKeycloakId(), request);
        keycloakUserService.updateRegistrationStatus(user.getKeycloakId(), RegisterStatus.PROFILE.name());
    }

    @Override
    @Transactional
    public void updateKycData(String keycloakId, UserKycDataRequest request) {
        User user = findByKeycloakId(keycloakId);

        validateStep(user, RegisterStatus.PROFILE);

        if (userKycRepository.existsByCurpIgnoreCase(request.getCurp())) {
            throw new DuplicateUserDataException("CURP");
        }
        if (userKycRepository.existsByRfcIgnoreCase(request.getRfc())) {
            throw new DuplicateUserDataException("RFC");
        }

        UserKyc userKyc = userKycRepository.findById(user.getId())
                .orElse(new UserKyc());

        userKyc.setUser(user);
        userKyc.setCurp(request.getCurp());
        userKyc.setRfc(request.getRfc());
        userKyc.setDocumentType(request.getDocumentType());
        userKycRepository.save(userKyc);

        user.setRole(UserRole.CUSTOMER_BASIC);
        user.setRegisterStatus(RegisterStatus.KYC);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Same transaction as the KYC: the account is requested only if the KYC is committed.
        accountRequestOutbox.enqueue(user);
        // Hit the DB constraints before touching Keycloak; if Keycloak fails, everything above rolls back.
        userRepository.flush();

        keycloakUserService.assignRole(user.getKeycloakId(), UserRole.CUSTOMER_BASIC);
        keycloakUserService.updateRegistrationStatus(user.getKeycloakId(), RegisterStatus.KYC.name());
    }

    @Override
    public void updateFinancialInfo(String keycloakId, UserFinancialInfoRequest request) {
        User user = findByKeycloakId(keycloakId);

        validateStep(user, RegisterStatus.KYC);

        UserFinancialInfo userFinancialInfo = userFinancialInfoRepository.findById(user.getId())
                .orElse(new UserFinancialInfo());

        userFinancialInfo.setUser(user);
        userFinancialInfo.setOccupationType(request.getOccupationType());
        userFinancialInfo.setEmployerName(request.getEmployerName());
        userFinancialInfo.setIncomeSource(request.getIncomeSource());
        userFinancialInfo.setMonthlyIncome(request.getMonthlyIncome());
        userFinancialInfo.setMaritalStatus(request.getMaritalStatus());
        userFinancialInfoRepository.save(userFinancialInfo);

        user.setRegisterStatus(RegisterStatus.COMPLETE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        keycloakUserService.updateRegistrationStatus(user.getKeycloakId(), RegisterStatus.COMPLETE.name());
    }

    private User findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException("keycloakId", keycloakId));
    }

    private void validateStep(User user, RegisterStatus expectedStatus) {
        if (user.getRegisterStatus() != expectedStatus) {
            throw new InvalidRegistrationStepException(
                    user.getRegisterStatus(),
                    expectedStatus);
        }
    }
}
