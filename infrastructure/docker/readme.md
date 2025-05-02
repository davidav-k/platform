# Docker Infrastructure

This directory contains the Docker setup for the Task Management Platform. Each microservice is containerized, allowing for easy deployment and scaling. Docker Compose is used to manage the entire stack for local development.

## Services:
- **User Service**: Handles user registration, authentication, and role management
- **Task Service**: Handles task creation, editing, and deletion
- **Notification Service**: Sends notifications to users when tasks are updated
- **Redis**: Stores access tokens for user authentication
- **PostgreSQL**: Stores user and task data

## Running the project:
To start all services locally, run the following command from the root of the repository:

```bash
docker-compose up --build

