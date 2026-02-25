package dev.enrique.bank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;

import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;
import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dto.request.RegistrationRequest;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.impl.RegistrationServiceImpl;
import dev.enrique.bank.service.util.UserHelper;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserHelper userHelper;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    private RegistrationRequest request;

    @BeforeEach
    void setUp() {
        request = new RegistrationRequest();
        request.setEmail("test@email.com");
        request.setPhoneCode("+52");
        request.setPhoneNumber("1234567890");
        request.setNames("Juan");
        request.setFirstSurname("Pérez");
        request.setSecondSurname("López");
        request.setGender(Gender.MALE);
        request.setBirthday("29/02/2000");
        request.setCountryOfBirth(Country.MX);
        request.setCurp("AAAA900101HDFRRN09");
        request.setRfc("AAAA900101AAA");
    }

    @Test
    void shouldReturnNewUser() {
        when(userRepository.getUserByEmail(request.getEmail(), User.class)).thenReturn(Optional.empty());
        String result = registrationService.registration(request, bindingResult);

        assertEquals("User registered", result);
        verify(userRepository).save(any(User.class));
        verify(userHelper).processInputErrors(bindingResult);
        verify(userHelper).ensureUserIdentifierAreUnique(request);
    }

    @Test
    void shouldUpdateInactiveUser() {
        User existingUser = new User();
        existingUser.setEmail(request.getEmail());
        existingUser.setActive(false);

        when(userRepository.getUserByEmail(request.getEmail(), User.class))
                .thenReturn(Optional.of(existingUser));

        String result = registrationService.registration(request, bindingResult);

        assertEquals("User updated", result);
        verify(userRepository).save(existingUser);
    }

    @Test
    void shouldThrowExceptionWhenUserAlreadyActive() {
        User existingUser = new User();
        existingUser.setEmail(request.getEmail());
        existingUser.setActive(true);

        when(userRepository.getUserByEmail(request.getEmail(), User.class))
                .thenReturn(Optional.of(existingUser));

        ApiRequestException ex = assertThrows(ApiRequestException.class,
                () -> registrationService.registration(request, bindingResult));

        assertEquals("Email has already be taken", ex.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(userRepository, never()).save(any());
    }
}
