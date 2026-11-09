package dev.enrique.bank.service.impl;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.commons.exception.DuplicateUserDataException;
import dev.enrique.bank.commons.exception.EmailDeliveryException;
import dev.enrique.bank.commons.exception.EmailNotVerifiedException;
import dev.enrique.bank.commons.exception.InvalidRegistrationStepException;
import dev.enrique.bank.commons.exception.UserNotFoundException;
import dev.enrique.bank.dao.UserFinancialInfoRepository;
import dev.enrique.bank.dao.UserKycRepository;
import dev.enrique.bank.dao.UserProfileRepository;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.broker.Topics;
import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.broker.outbox.EventOutbox;
import dev.enrique.bank.commons.enums.AccountType;
import dev.enrique.bank.commons.enums.Currency;
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
import dev.enrique.bank.commons.util.RollbackCompensation;
import dev.enrique.bank.commons.validation.KycConsistency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserKycRepository userKycRepository;
    private final UserFinancialInfoRepository userFinancialInfoRepository;
    private final KeycloakUserService keycloakUserService;
    private final EventOutbox eventOutbox;
    private final BasicMapper basicMapper;

    @Override
    @Transactional
    public UserRegisterResponse register(UserRegisterRequest request) {
        // Checked before Keycloak so a duplicate never creates (and then has to delete) a Keycloak user.
        // A concurrent duplicate still hits the DB unique constraint and is compensated below.
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateUserDataException("email");
        }
        if (userRepository.existsByPhoneCodeAndPhoneNumber(request.getPhoneCode(), request.getPhoneNumber())) {
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

        // Any failure from here on, the commit included, deletes the Keycloak user: an orphan would block
        // registering again with the same email.
        RollbackCompensation.onRollback("delete Keycloak user " + keycloakId,
                () -> keycloakUserService.deleteUser(keycloakId));
        userRepository.saveAndFlush(user);
        keycloakUserService.updateRegistrationStatus(keycloakId, RegisterStatus.REGISTER.name());

        // Only once the user exists: a rolled back registration never gets a verification email.
        // Best effort: if the email cannot be sent now, the user can ask for it again.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    keycloakUserService.sendVerificationEmail(keycloakId);
                } catch (EmailDeliveryException ex) {
                    log.warn("Verification email for user {} could not be sent: {}", keycloakId,
                            ex.getCause().toString());
                }
            }
        });

        return basicMapper.convertToResponse(user, UserRegisterResponse.class);
    }

    @Override
    @Transactional
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

        // Hit the DB before touching Keycloak; if Keycloak fails, the step is rolled
        // back and can be retried.
        userRepository.saveAndFlush(user);

        // The DB rollback does not reach Keycloak: put back the name and step the user had.
        String userKeycloakId = user.getKeycloakId();
        RollbackCompensation.onRollback("undo profile of user " + userKeycloakId, () -> {
            keycloakUserService.updateName(userKeycloakId, KeycloakUserServiceImpl.PENDING_NAME,
                    KeycloakUserServiceImpl.PENDING_NAME);
            keycloakUserService.updateRegistrationStatus(userKeycloakId, RegisterStatus.REGISTER.name());
        });
        keycloakUserService.updateUser(userKeycloakId, request);
        keycloakUserService.updateRegistrationStatus(userKeycloakId, RegisterStatus.PROFILE.name());
    }

    @Override
    @Transactional
    public void updateKycData(String keycloakId, UserKycDataRequest request) {
        User user = findByKeycloakId(keycloakId);

        validateStep(user, RegisterStatus.PROFILE);

        // The account is opened at this step, so the email must be proven first.
        if (!keycloakUserService.isEmailVerified(user.getKeycloakId())) {
            throw new EmailNotVerifiedException();
        }

        // The profile step (birthday) is done: validateStep only lets PROFILE users through.
        UserProfile profile = userProfileRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("User " + user.getId() + " has no profile"));
        KycConsistency.check(request.getCurp(), request.getRfc(), profile.getBirthday());

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

        // Same transaction as the KYC: the account is requested only if the KYC is
        // committed.
        eventOutbox.enqueue(Topics.ACCOUNT_REQUESTED, user.getKeycloakId(), CreateAccountEvent.builder()
                .email(user.getEmail())
                .accountType(Objects.requireNonNullElse(request.getAccountType(), AccountType.SAVING))
                .currency(Objects.requireNonNullElse(request.getCurrency(), Currency.MX))
                .build());

        // Hit the DB constraints before touching Keycloak; if Keycloak fails,
        // everything above rolls back.
        userRepository.flush();

        // The DB rollback does not reach Keycloak: without this, a failure after assignRole
        // leaves CUSTOMER_BASIC (accepted by every service) on a user that never passed KYC.
        String userKeycloakId = user.getKeycloakId();
        RollbackCompensation.onRollback("undo KYC of user " + userKeycloakId, () -> {
            keycloakUserService.removeRole(userKeycloakId, UserRole.CUSTOMER_BASIC);
            keycloakUserService.updateRegistrationStatus(userKeycloakId, RegisterStatus.PROFILE.name());
        });
        keycloakUserService.assignRole(user.getKeycloakId(), UserRole.CUSTOMER_BASIC);
        keycloakUserService.updateRegistrationStatus(user.getKeycloakId(), RegisterStatus.KYC.name());
    }

    @Override
    @Transactional
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
        userRepository.saveAndFlush(user);

        String userKeycloakId = user.getKeycloakId();
        RollbackCompensation.onRollback("undo financial step of user " + userKeycloakId,
                () -> keycloakUserService.updateRegistrationStatus(userKeycloakId, RegisterStatus.KYC.name()));
        keycloakUserService.updateRegistrationStatus(userKeycloakId, RegisterStatus.COMPLETE.name());
    }

    @Override
    public void resendVerificationEmail(String keycloakId) {
        User user = findByKeycloakId(keycloakId);
        if (!keycloakUserService.isEmailVerified(user.getKeycloakId())) {
            keycloakUserService.sendVerificationEmail(user.getKeycloakId());
        }
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
