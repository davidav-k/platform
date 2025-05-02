# Task Service

The Task Service is responsible for managing user tasks, including creating, editing, and deleting tasks. It supports full CRUD operations and integrates with the User Service for role-based access control.

## Features:
- Create, update, delete tasks
- Associate tasks with users
- Support for task priority and status
- Role-based access control (users can only manage their own tasks)

## Technologies:
- Spring Boot
- PostgreSQL for task storage
- Kafka for message exchange with other microservices (optional)
