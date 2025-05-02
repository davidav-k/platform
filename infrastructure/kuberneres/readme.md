# Kubernetes Infrastructure

This directory contains the Kubernetes deployment configurations for the Task Management Platform. These configurations include Helm charts and Kubernetes manifests for deploying the platform's microservices to a Kubernetes cluster.

## Services:
- **User Service**: Handles user management
- **Task Service**: Handles task operations
- **Notification Service**: Manages notifications
- **Redis**: Stores access tokens for authentication
- **PostgreSQL**: Database for storing user and task data

## How to deploy:
1. Install Helm if you haven't already:
   ```bash
   brew install helm