package com.example.user_service.event.listener;

import com.example.user_service.event.UserEvent;
import com.example.user_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final EmailService emailService;

    @EventListener
    public void onUserEvent(UserEvent userEvent) {
        switch (userEvent.getType()) {
            case REGISTRATION -> emailService.sendNewAccountEmail(userEvent.getUser().getFirstName(), userEvent.getUser().getEmail(), (String) userEvent.getData().get("key"));
            case RESETPASSWORD -> emailService.sendPasswordResetEmail(userEvent.getUser().getFirstName(), userEvent.getUser().getEmail(), (String) userEvent.getData().get("key"));
            default -> {}
        }
    }
}

