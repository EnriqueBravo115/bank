package dev.enrique.bank;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import dev.enrique.bank.pojo.entity.User;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.service.impl.UserServiceImpl;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class ApiApplicationTests {

    @MockBean
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void testFindUserById() {
        User mock_user = mock(User.class);

        Optional<User> optional_user = Optional.of(mock_user);

        when(mock_user.getId()).thenReturn(1L);
        when(mock_user.getUsername()).thenReturn("john.doe");
        when(mock_user.getEmail()).thenReturn("john.doe@example.com");
        when(mock_user.getPassword()).thenReturn("password");

        when(userRepository.findById(1L)).thenReturn(optional_user);

        verify(userRepository).findById(1L);
    }
}
