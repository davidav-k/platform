package com.example.ai_service.usecase;

import com.example.ai_service.model.ImproveTaskDescriptionRequest;
import com.example.ai_service.model.ImproveTaskDescriptionResult;

public interface ImproveTaskDescriptionUseCase {

    ImproveTaskDescriptionResult improveTaskDescription(ImproveTaskDescriptionRequest request);
}
