package com.example.task_service;

import com.example.task_service.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies that the Spring application context loads successfully.
 */
@SpringBootTest
@ActiveProfiles("test")
class TaskServiceApplicationTests {

    @MockitoBean
    private TaskRepository taskRepository;

    @Test
    void contextLoads() {
    }

}
