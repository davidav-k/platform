package com.example.ai_service.usecase;

import com.example.ai_service.model.SummarizeTaskRequest;
import com.example.ai_service.model.SummarizeTaskResult;

public interface SummarizeTaskUseCase {

    SummarizeTaskResult summarizeTask(SummarizeTaskRequest request);
}
