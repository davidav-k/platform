package com.example.user_service.utils;

import com.example.user_service.domain.RequestContext;
import com.example.user_service.entity.CredentialEntity;
import com.example.user_service.entity.RoleEntity;
import com.example.user_service.entity.UserEntity;
import com.example.user_service.enumeration.Authority;
import com.example.user_service.exception.ApiException;
import com.example.user_service.repository.CredentialRepository;
import com.example.user_service.repository.RoleRepository;
import com.example.user_service.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;


    @Value("${admin.password}")
    private String adminPassword;

    @PostConstruct
    public void initAdminUser() {
        if (!userRepository.existsByEmail("admin@mail.com")) {
            RoleEntity adminRole = roleRepository.findByNameIgnoreCase("ADMIN")
                    .orElseGet(() -> {
                        RoleEntity role = new RoleEntity();
                        role.setName("ADMIN");
                        role.setAuthorities(Authority.ADMIN);
                        RequestContext.setUserId(0L);
                        return roleRepository.save(role);
                    });
            UserEntity admin = UserUtils.createUserEntity("admin", "admin", "admin@mail.com", adminRole);
            admin.setEnabled(true);
            RequestContext.setUserId(0L);
            userRepository.save(admin);
            CredentialEntity credentialEntity = new CredentialEntity(admin, passwordEncoder.encode(adminPassword));
            credentialRepository.save(credentialEntity);
            log.info("Admin user created successfully!");
        }
    }
}

