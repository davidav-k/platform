# API Gateway Service
### Overview
This service acts as a centralized entry point for the microservice architecture, 
handling routing, authentication, and cross-cutting concerns like CORS and circuit breaking.

### Features
- JWT-based authentication
- Dynamic service discovery via Eureka
- Intelligent request routing
- Circuit breaker pattern implementation
- CORS configuration
- Path rewriting

### Technologies
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka Client
- JWT for authentication
- Resilience4j for fault tolerance

### Configuration

The API Gateway routes traffic to the following services:  
- User Service (/api/users/**)
- Task Service (/api/tasks/**)

### Authentication
All requests are validated using JWT tokens. The token must be provided in the Authorization 
header with the Bearer scheme. The gateway extracts the username and passes it 
as an X-Authenticated-User header to downstream services.

### API Routes
- User Service: http://localhost:8080/api/users/**
- Task Service: http://localhost:8080/api/tasks/**