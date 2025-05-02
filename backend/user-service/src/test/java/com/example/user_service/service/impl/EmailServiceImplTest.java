package com.example.user_service.service.impl;

import com.example.user_service.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    private final String testHost = "http://localhost:8080";
    private final String testEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "host", testHost);
        ReflectionTestUtils.setField(emailService, "fromEmail", testEmail);
    }

    @Test
    void sendNewAccountEmailSuccessfully() {

        String name = "user";
        String email = "user@mail.com";
        String key = "abc123";

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.sendNewAccountEmail(name, email, key));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendNewAccountEmailFails() {

        String name = "user";
        String email = "user@mail.com";
        String key = "abc123";

        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        ApiException exception = assertThrows(ApiException.class, () ->
                emailService.sendNewAccountEmail(name, email, key));
        assertEquals("Unable to send email", exception.getMessage());
    }

    @Test
    void sendPasswordResetEmailSuccessfully() {

        String name = "user";
        String email = "user@mail.com";
        String token = "abc123";

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.sendPasswordResetEmail(name, email, token));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendPasswordResetEmailFails() {

        String name = "user";
        String email = "user@mail.com";
        String token = "abc123";

        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        ApiException exception = assertThrows(ApiException.class, () ->
                emailService.sendPasswordResetEmail(name, email, token));
        assertEquals("Unable to send email", exception.getMessage());
    }

@Test
void sendNewAccountEmailWithNullParameters() {

    doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

    ApiException exception = assertThrows(ApiException.class, () ->
            emailService.sendNewAccountEmail(null, null, null));
    assertEquals("Unable to send email", exception.getMessage());
}

@Test
void sendPasswordResetEmailWithNullParameters() {

    doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

    ApiException exception = assertThrows(ApiException.class, () ->
            emailService.sendPasswordResetEmail(null, null, null));
    assertEquals("Unable to send email", exception.getMessage());
}
}