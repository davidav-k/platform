# Platform

## Project Overview
Task Management Platform is a microservice-based system for managing tasks, with features for user authentication, task creation, editing, and notifications. The platform implements modern security measures with **access** and **refresh tokens** to ensure secure user authentication and authorization. **Access tokens** are stored in **Redis** for fast, scalable, and secure token management.

The project utilizes the following technologies:
- **Backend**: Spring Boot microservices (User Service, Task Service, Notification Service)
- **Frontend**: Vue.js for a dynamic user interface
- **Infrastructure**: Docker for containerization, Kubernetes for orchestration, GitHub Actions for CI/CD, Kafka for messaging between services

## Key Features:
- **Authentication and Authorization**: User authentication using JWT-based **access** and **refresh tokens**. Access tokens have a short lifespan and are stored in **Redis** for efficient retrieval. Refresh tokens are used to renew access tokens without requiring re-login.
- **Task Management**: Full CRUD operations for tasks (create, update, delete) with role-based access.
- **Notifications**: Real-time notifications about task updates or comments.
- **Caching and Monitoring**: Redis is used for caching access tokens, ensuring efficient data access and high performance, and monitoring is implemented for microservices.

## Technology Stack:
- **Backend**: Java, Spring Boot
- **Frontend**: Vue.js
- **Database**: PostgreSQL
- **Infrastructure**: Docker, Kubernetes, GitHub Actions, Kafka, Redis
