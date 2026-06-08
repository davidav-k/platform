package com.example.task_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Task Service microservice.
 *
 * <p>This service is responsible for task management within the platform.
 * It registers with Eureka and loads configuration from Config Server.
 */
@SpringBootApplication
public class TaskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }

}
