package com.example.user_service.integration;

import com.example.user_service.UserServiceApplication;
import com.example.user_service.config.TestContainersConfig;
import com.example.user_service.config.TestEmailConfig;
import com.example.user_service.config.TestSecurityConfig;
import com.example.user_service.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(
    initializers = {TestContainersConfig.Initializer.class},
        classes = {UserServiceApplication.class, TestContainersConfig.class, TestEmailConfig.class, TestSecurityConfig.class}
)
public class UserServiceIntegrationTest {

    @MockBean
    private SecurityConfig securityConfig;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }
}