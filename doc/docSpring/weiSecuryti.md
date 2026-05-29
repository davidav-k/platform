### Потоки выполнения успешных и неуспешных запросов

---

## 1. **Запрос на авторизацию**

### Успешный запрос:
1. **Клиент:**
   - Отправляет запрос на авторизацию (например, `/auth/login`) с учетными данными.
     ```http
     POST /auth/login HTTP/1.1
     Content-Type: application/json
     {
       "username": "user",
       "password": "password"
     }
     ```

2. **DispatcherServlet:**
   - Направляет запрос в цепочку фильтров.

3. **Spring Security Filter Chain:**
   - `UsernamePasswordAuthenticationFilter`:
     - Проверяет учетные данные.
     - Если данные валидны, создает объект `Authentication` и сохраняет его в `SecurityContext`.
   - Токен JWT генерируется и возвращается клиенту.

4. **Контроллер авторизации:**
   - Возвращает токен клиенту:
     ```json
     {
       "token": "<jwt_token>"
     }
     ```

---

### Неуспешный запрос:
1. **Клиент:**
   - Отправляет запрос с неверными учетными данными.

2. **DispatcherServlet:**
   - Направляет запрос в цепочку фильтров.

3. **Spring Security Filter Chain:**
   - `UsernamePasswordAuthenticationFilter`:
     - Проверяет учетные данные.
     - Если учетные данные недействительны, выбрасывает `BadCredentialsException`.

4. **ExceptionHandlerAdvice:**
   - Обрабатывает исключение и возвращает ответ:
     ```json
     {
       "flag": false,
       "code": 401,
       "message": "username or password is incorrect."
     }
     ```

---

## 2. **Запрос на защищенный эндпоинт**

---

### Успешный запрос (с токеном):
1. **Клиент:**
   - Отправляет запрос с токеном:
     ```http
     GET /users/123 HTTP/1.1
     Authorization: Bearer <valid_token>
     ```

2. **DispatcherServlet:**
   - Направляет запрос в цепочку фильтров.

3. **Spring Security Filter Chain:**
   - `BearerTokenAuthenticationFilter`:
     - Извлекает токен из заголовка.
     - Проверяет подпись и срок действия токена.
     - Сохраняет объект `Authentication` в `SecurityContext`.

4. **JwtInterceptor:**
   - Проверяет, находится ли токен в белом списке Redis.

5. **UserRequestAuthorizationManager:**
   - Проверяет права доступа:
     - Если `ROLE_admin` или пользователь имеет доступ к ресурсу (`userId` совпадает), запрос передается контроллеру.

6. **Контроллер:**
   - Обрабатывает запрос и возвращает данные:
     ```json
     {
       "flag": true,
       "code": 200,
       "message": "Success",
       "data": { "id": 123, "name": "John Doe" }
     }
     ```

---

### Неуспешный запрос (с неверным токеном):
1. **Клиент:**
   - Отправляет запрос с недействительным токеном.

2. **Spring Security Filter Chain:**
   - `BearerTokenAuthenticationFilter`:
     - Проверяет токен.
     - Если токен недействителен, возвращает ошибку `401 Unauthorized` через `CustomBearerTokenAuthenticationEntryPoint`.

3. **ExceptionHandlerAdvice:**
   - Формирует ответ:
     ```json
     {
       "flag": false,
       "code": 401,
       "message": "The access token provided is expired, revoked, malformed, or invalid for other reasons."
     }
     ```

---

### Неуспешный запрос (без токена):
1. **Клиент:**
   - Отправляет запрос без заголовка `Authorization`.

2. **Spring Security Filter Chain:**
   - `BearerTokenAuthenticationFilter`:
     - Обнаруживает отсутствие токена.
     - Возвращает ошибку `401 Unauthorized` через `CustomBearerTokenAuthenticationEntryPoint`.

3. **ExceptionHandlerAdvice:**
   - Формирует ответ:
     ```json
     {
       "flag": false,
       "code": 401,
       "message": "Login credentials are missing."
     }
     ```

---

## 3. **Запрос на незащищенный эндпоинт**

---

### Успешный запрос:
1. **Клиент:**
   - Отправляет запрос:
     ```http
     GET /artifacts/123 HTTP/1.1
     ```

2. **DispatcherServlet:**
   - Направляет запрос в цепочку фильтров.

3. **Spring Security Filter Chain:**
   - Пропускает запрос, так как маршрут разрешен в конфигурации:
     ```java
     .requestMatchers("/artifacts/**").permitAll()
     ```

4. **Контроллер:**
   - Обрабатывает запрос и возвращает данные:
     ```json
     {
       "flag": true,
       "code": 200,
       "message": "Artifact retrieved successfully.",
       "data": { "id": 123, "name": "Artifact A" }
     }
     ```

---

### Неуспешный запрос (ошибка в контроллере):
1. **Клиент:**
   - Отправляет запрос, но контроллер выбрасывает исключение, например `ObjectNotFoundException`.

2. **ExceptionHandlerAdvice:**
   - Перехватывает исключение и формирует ответ:
     ```json
     {
       "flag": false,
       "code": 404,
       "message": "This API endpoint is not found."
     }
     ```

---

### Сравнение потоков

| **Тип запроса**            | **Компоненты**                                                                                     | **Тип ответа**                                                                 |
|-----------------------------|---------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| Авторизация                | `UsernamePasswordAuthenticationFilter`, `Controller`, `ExceptionHandlerAdvice`                   | Успех: токен; Ошибка: `401 Unauthorized`                                       |
| Защищенный эндпоинт        | `BearerTokenAuthenticationFilter`, `JwtInterceptor`, `UserRequestAuthorizationManager`, `Controller`, `ExceptionHandlerAdvice` | Успех: данные; Ошибка: `401 Unauthorized` или `403 Forbidden`                 |
| Незащищенный эндпоинт      | `Spring Security Filter Chain` (без проверки), `Controller`, `ExceptionHandlerAdvice`           | Успех: данные; Ошибка: `404 Not Found` (если контроллер не обрабатывает запрос) |

---

Если нужны уточнения или схемы выполнения, дайте знать! 😊
