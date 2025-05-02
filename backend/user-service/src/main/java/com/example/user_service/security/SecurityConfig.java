package com.example.user_service.security;

import com.example.user_service.service.JwtService;
import com.example.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.List;

import static com.example.user_service.constant.Constants.*;


@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${api.endpoint.base-url}")
    private String baseUrl;

    private static final String[] SWAGGER_WHITELIST = {
            "/v3/api-docs/**",       // OpenAPI documentation
            "/swagger-ui/**",        // Swagger UI
            "/swagger-ui.html",      // Swagger UI HTML
    };

    private static final String[] H2_CONSOLE_WHITELIST = {
            "/h2-console/**"  // H2 database console for testing
    };

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserService userService) {
        return new ProviderManager(List.of(new ApiAuthenticationProvider(userService, passwordEncoder())));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager, UserService userService, JwtService jwtService) throws Exception {
        AuthenticationFilter authenticationFilter = new AuthenticationFilter(baseUrl + "/user/login", authenticationManager, userService, jwtService);
        authenticationFilter.setAuthenticationManager(authenticationManager);

        return http
                .authorizeHttpRequests(authorize -> authorize
//                        .requestMatchers(HttpMethod.POST, baseUrl + "/user/register").permitAll()
                        .requestMatchers(HttpMethod.GET, baseUrl + "/user/verify/account").permitAll()
                        .requestMatchers(HttpMethod.POST, baseUrl + "/user/login").permitAll()
                        .requestMatchers(SWAGGER_WHITELIST).permitAll()
                        .requestMatchers(H2_CONSOLE_WHITELIST).permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)) // Allow H2 console
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authenticationFilter, JwtAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }
}
