# User Service

This service handles user management, including:
- User registration
- Authentication using JWT
- Role management

## Technologies
- Spring Boot
- PostgreSQL for user data storage
- Google Guava for caching
- Spring Security for authentication and authorization
- Spring Mail for email notifications
- Docker for containerization
- JUnit and Mockito for testing
- Lombok for reducing boilerplate code

## Endpoints and Permissions

### User Management
- **POST** `/api/v1/user/register` - Register a new user. **Requires:** `user:create`
- **GET** `/api/v1/user/verify/account` - Verify a new user account using a key. **Requires:** No authentication
- **POST** `/api/v1/user/login` - Log in a user. **Requires:** No authentication
- **POST** `/api/v1/user/enable-mfa` - Enable multi-factor authentication (MFA) for a user. **Requires:** `user:update`  or be the owner of the account
- **POST** `/api/v1/user/verify-mfa` - Verify MFA for a user. **Requires:** No authentication
- **POST** `/api/v1/user/unlock` - Unlock a user account. **Requires:** `user:update`
- **POST** `/api/v1/user/lock` - Lock a user account. **Requires:** `user:update`
- **POST** `/api/v1/user/refresh` - Refresh access and refresh tokens. **Requires:** Valid refresh token
- **GET** `/api/v1/user/profile` - Retrieve the profile of the logged-in user. **Requires:** Valid access token
- **PUT** `/api/v1/user/{userId}` - Update user details. **Requires:** `user:update` or be the owner of the account
- **PATCH** `/api/v1/user/password/{userId}` - Change a user's password. **Requires:** `user:update` or be the owner of the account
- **DELETE** `/api/v1/user/{userId}` - Delete a user. **Requires:** `user:delete`

### For local launch
1. .env file
   - Copy .env.example to .env
   - Change the values of the variables in the .env file to your local environment
     - Make sure to set your ip address:
       - POSTGRES_HOST
       - EMAIL_HOST

2. Delete all containers and volumes (docker)
3. Run in the terminal from the root project folder
```bash
docker compose --env-file .env -f docker/compose.yml up -d
```
Containers with the database, mail service and admin panel are built and launched
4. Launch the application.

#### localhost:7001 - admin panel (pgAdmin) postgresql
#### localhost:8025 - mailhog (email testing tool)