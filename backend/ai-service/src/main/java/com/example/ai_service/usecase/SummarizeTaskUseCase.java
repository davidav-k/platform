package com.example.ai_service.usecase;

import com.example.ai_service.model.TaskSummaryRequest;
import com.example.ai_service.model.TaskSummaryResponse;

public interface SummarizeTaskUseCase {

    TaskSummaryResponse summarizeTask(TaskSummaryRequest request);
}
