package com.example.ai_service.usecase;

import com.example.ai_service.model.TaskDescriptionImprovementRequest;
import com.example.ai_service.model.TaskDescriptionImprovementResponse;

public interface ImproveTaskDescriptionUseCase {

    TaskDescriptionImprovementResponse improveTaskDescription(TaskDescriptionImprovementRequest request);
}
