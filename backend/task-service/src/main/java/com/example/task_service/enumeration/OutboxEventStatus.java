package com.example.task_service.enumeration;

public enum OutboxEventStatus {
    NEW,
    PROCESSING,
    PROCESSED,
    FAILED
}
