package com.example.user_service.event;

import com.example.user_service.entity.UserEntity;
import com.example.user_service.enumeration.EventType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class UserEvent {

    private UserEntity user;

    private EventType type;

    private Map<?,?> data;
}
