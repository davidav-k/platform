# Notification Service

The Notification Service is responsible for sending notifications to users when certain events occur in the system, such as task updates or comments. It can send notifications via email, push notifications, or other channels.

## Features:
- Send notifications when tasks are created, updated, or deleted
- Notification types: email, push notifications
- Integrates with Kafka for receiving events from other microservices

## Technologies:
- Spring Boot
- Kafka for receiving task events
- Redis (optional) for caching notification events
