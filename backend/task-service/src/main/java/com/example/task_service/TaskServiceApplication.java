package com.example.task_service;

import com.example.task_service.notification.NotificationClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry point for the Task Service microservice.
 *
 * <p>This service is responsible for task management within the platform.
 * It registers with Eureka and loads configuration from Config Server.
 */
@SpringBootApplication
@EnableConfigurationProperties(NotificationClientProperties.class)
public class TaskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }

}
