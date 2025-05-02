package com.example.user_service.service.impl;

import com.example.user_service.exception.ApiException;
import com.example.user_service.service.EmailService;
import com.example.user_service.utils.EmailUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String NEW_USER_ACCOUNT_VERIFICATION = "New user account verification";

    private static final String PASSWORD_RESET_REQUEST = "Password reset request";

    private final JavaMailSender sender;

    @Value("${spring.mail.verify.host}")
    private String host;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Async
    public void sendNewAccountEmail(String name, String email, String key) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setSubject(NEW_USER_ACCOUNT_VERIFICATION);
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setText(EmailUtils.getEmailMessage(name, host, key));
            sender.send(message);
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Unable to send email");
        }
    }


    @Override
    @Async
    public void sendPasswordResetEmail(String name, String email, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setSubject(PASSWORD_RESET_REQUEST);
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setText(EmailUtils.getResetPasswordMessage(name, host, token));
            sender.send(message);
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Unable to send email");
        }
    }
}


