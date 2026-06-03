package com.jadakeel.auth.controller;

import com.jadakeel.auth.dto.AuthResponse;
import com.jadakeel.auth.dto.RegisterRequest;
import com.jadakeel.auth.model.UserAccount;
import com.jadakeel.auth.repository.UserAccountRepository;
import com.jadakeel.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private JwtService jwtService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(userRepository, jwtService);
    }

    @Test
    void register_alwaysCreatesCustomerAndNormalizesEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName(" Test User ");
        request.setEmail("TEST@Example.COM ");
        request.setPassword("strong-password");
        request.setRole(UserAccount.Role.ADMIN);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateToken(any(UserAccount.class))).thenReturn("token");

        AuthResponse response = authController.register(request);

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).save(userCaptor.capture());
        UserAccount savedUser = userCaptor.getValue();

        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals(UserAccount.Role.CUSTOMER, savedUser.getRole());
        assertNotEquals(UserAccount.Role.ADMIN, response.getRole());
    }
}
