package com.example.user_service.integration;

import com.example.user_service.UserServiceApplication;
import com.example.user_service.config.TestEmailConfig;
import com.example.user_service.config.TestSecurityConfig;
import com.example.user_service.dto.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.SecurityConfig;
import com.example.user_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=optional:configserver:",
    "eureka.client.enabled=false",
    "eureka.client.register-with-eureka=false",
    "eureka.client.fetch-registry=false"
})
@ActiveProfiles("test")
@ContextConfiguration(classes = {
    UserServiceApplication.class,
    TestEmailConfig.class,
    TestSecurityConfig.class
})
@Testcontainers
class UserServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @MockBean
    private SecurityConfig securityConfig;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoads() {
        assertThat(userService).isNotNull();
    }

    @Test
    @Transactional
    void deleteUserRemovesUserServiceOwnedDependentRows() {
        long targetUserId = 900L;
        jdbcTemplate.update("""
                INSERT INTO users
                    (id, user_id, reference_id, created_at, created_by, updated_at, updated_by, email,
                     first_name, last_name, login_attempts, mfa, enabled, account_non_expired, account_non_locked)
                VALUES
                    (?, ?, ?, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, ?, ?, ?, 0, FALSE, TRUE, TRUE, TRUE)
                """, targetUserId, "deletion-target-public-id", "deletion-target", "delete-target@example.com", "Delete", "Target");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, 2)", targetUserId);
        jdbcTemplate.update("""
                INSERT INTO credentials
                    (id, reference_id, created_at, created_by, updated_at, updated_by, password, user_id)
                VALUES
                    (?, ?, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, ?, ?)
                """, 901L, "deletion-credential", "encoded-password", targetUserId);
        jdbcTemplate.update("""
                INSERT INTO confirmations
                    (id, reference_id, created_at, created_by, updated_at, updated_by, "key", user_id)
                VALUES
                    (?, ?, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, ?, ?)
                """, 902L, "deletion-confirmation", "deletion-key", targetUserId);
        jdbcTemplate.update("""
                INSERT INTO login_history
                    (id, reference_id, created_at, created_by, updated_at, updated_by, user_id, login_time, success)
                VALUES
                    (?, ?, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, ?, CURRENT_TIMESTAMP, TRUE)
                """, 903L, "deletion-login-history", targetUserId);

        User adminUser = User.builder().id(999L).build();
        userService.deleteUser(targetUserId, new UsernamePasswordAuthenticationToken(adminUser, null));
        userRepository.flush();

        assertThat(countRowsById("users", targetUserId)).isZero();
        assertThat(countRowsByUserId("credentials", targetUserId)).isZero();
        assertThat(countRowsByUserId("confirmations", targetUserId)).isZero();
        assertThat(countRowsByUserId("login_history", targetUserId)).isZero();
        assertThat(countRowsByUserId("user_roles", targetUserId)).isZero();
    }

    private Integer countRowsById(String table, long id) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id);
    }

    private Integer countRowsByUserId(String table, long userId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE user_id = ?", Integer.class, userId);
    }
}
