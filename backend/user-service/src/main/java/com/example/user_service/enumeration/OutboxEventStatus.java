package com.example.user_service.enumeration;

public enum OutboxEventStatus {
    NEW,
    PROCESSING,
    PROCESSED,
    FAILED
}
