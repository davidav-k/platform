package com.example.ai_service.outbox;

public final class AiOutboxEventTypes {

    public static final String TASK_DESCRIPTION_IMPROVED = "AI_TASK_DESCRIPTION_IMPROVED";
    public static final String SUBTASKS_SUGGESTED = "AI_SUBTASKS_SUGGESTED";
    public static final String TASK_SUMMARIZED = "AI_TASK_SUMMARIZED";
    public static final String PRIORITY_SUGGESTED = "AI_PRIORITY_SUGGESTED";

    private AiOutboxEventTypes() {
    }
}
