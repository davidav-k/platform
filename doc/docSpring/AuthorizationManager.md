### Подробно о **`AuthorizationManager`** в Spring Security

**`AuthorizationManager`** — это компонент Spring Security, который отвечает за **авторизацию**. Он определяет, имеет ли текущий пользователь доступ к ресурсу или операции, основываясь на его ролях, полномочиях и других атрибутах запроса.

---

### **Когда используется `AuthorizationManager`?**

- Используется для **проверки доступа** на уровне запроса (например, REST API).
- Является гибкой альтернативой аннотациям `@PreAuthorize`, `@Secured` или `hasRole`.
- Позволяет настраивать авторизацию с учетом атрибутов запроса, таких как:
  - Путь (`/users/{id}`)
  - Метод HTTP (`GET`, `POST`)
  - Текущий пользователь (из `Authentication`).

---

### **Основная роль `AuthorizationManager`**
1. Проверяет, разрешен ли доступ к защищенному ресурсу.
2. Работает **после аутентификации** — то есть, когда объект `Authentication` уже установлен в `SecurityContext`.
3. Включается в процесс потока выполнения запроса перед передачей управления контроллеру.

---

### **Где находится `AuthorizationManager` в потоке выполнения запроса?**

1. **После фильтров аутентификации:**
   - После того, как токен аутентифицирован (например, через `BearerTokenAuthenticationFilter`), создается объект `Authentication`, и он сохраняется в `SecurityContext`.

2. **Перед вызовом контроллера:**
   - `AuthorizationManager` вызывается для проверки прав доступа.
   - Если авторизация успешна, запрос передается в контроллер.
   - Если авторизация не удалась, запрос завершается с ошибкой `403 Forbidden`.

---

### **Как работает `AuthorizationManager`?**

1. **`AuthorizationManager` вызывается:**
   - Spring Security вызывает метод `check` у зарегистрированного `AuthorizationManager` для проверки доступа.
   - Метод принимает:
     - **`Authentication`** — текущего пользователя (взято из `SecurityContext`).
     - **`RequestAuthorizationContext`** — контекст запроса (например, URI, метод HTTP).

2. **Определение доступа:**
   - Проверяются условия доступа, например:
     - Роли пользователя (`ROLE_USER`, `ROLE_ADMIN`).
     - Соответствие пути (`/users/{id}`).
     - Дополнительные условия (например, пользователь может получить доступ только к своим данным).

3. **Возвращение решения:**
   - Возвращается объект `AuthorizationDecision`, содержащий результат проверки:
     - `true` — доступ разрешен.
     - `false` — доступ запрещен.

4. **Дальнейший поток:**
   - Если доступ разрешен, запрос передается в контроллер.
   - Если доступ запрещен, выбрасывается исключение `AccessDeniedException`.

---

### **Ключевые методы `AuthorizationManager`**

1. **`check(Supplier<Authentication> authentication, Object object)`**
   - Основной метод, который определяет, разрешен ли доступ к ресурсу.
   - Принимает:
     - `authentication` — текущий объект `Authentication` (например, токен JWT).
     - `object` — контекст запроса (например, `RequestAuthorizationContext`).
   - Возвращает:
     - `AuthorizationDecision`.

2. **`AuthorizationDecision`**
   - Представляет результат авторизации:
     - `true` — доступ разрешен.
     - `false` — доступ запрещен.

---

### **Пример реализации `AuthorizationManager`**

#### **Код `AuthorizationManager`**
```java
@Component
public class UserRequestAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    private static final UriTemplate USER_URI_TEMPLATE = new UriTemplate("/users/{userId}");

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext context) {
        // Извлекаем путь из URI
        Map<String, String> uriVariables = USER_URI_TEMPLATE.match(context.getRequest().getRequestURI());
        String userIdFromRequestUri = uriVariables.get("userId");

        // Получаем текущую аутентификацию
        Authentication authentication = authenticationSupplier.get();
        String userIdFromJwt = ((Jwt) authentication.getPrincipal()).getClaim("userId").toString();

        // Проверка ролей
        boolean hasUserRole = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_user"));
        boolean hasAdminRole = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_admin"));

        // Проверка совпадения userId
        boolean userIdsMatch = userIdFromRequestUri != null && userIdFromRequestUri.equals(userIdFromJwt);

        // Логика авторизации
        return new AuthorizationDecision(hasAdminRole || (hasUserRole && userIdsMatch));
    }
}
```

#### **Как это работает:**
- Если у пользователя роль `ROLE_admin`, он получает доступ ко всем ресурсам.
- Если у пользователя роль `ROLE_user`, он может получить доступ только к своим ресурсам (проверяется совпадение `userId`).

---

### **Пример конфигурации Security с `AuthorizationManager`**

#### Конфигурация Spring Security:
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRequestAuthorizationManager authorizationManager) throws Exception {
    return http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(HttpMethod.GET, "/users/**")
                .access(authorizationManager) // Используем кастомный AuthorizationManager
            .anyRequest()
                .authenticated()
        )
        .httpBasic(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .build();
}
```

---

### **Поток выполнения с `AuthorizationManager`**

1. **HTTP-запрос от клиента:**
   - Пример запроса:
     ```http
     GET /users/123 HTTP/1.1
     Authorization: Bearer <token>
     ```

2. **Аутентификация:**
   - Запрос проходит через `BearerTokenAuthenticationFilter`.
   - Токен проверяется, а объект `Authentication` сохраняется в `SecurityContext`.

3. **Авторизация:**
   - `AuthorizationManager` вызывается для проверки прав доступа.
   - Пример логики:
     - Если пользователь — администратор (`ROLE_admin`), доступ разрешен.
     - Если пользователь — обычный пользователь (`ROLE_user`), проверяется, совпадает ли `userId`.

4. **Результат:**
   - Если авторизация успешна, запрос передается в контроллер.
   - Если авторизация не удалась, возвращается ошибка `403 Forbidden`.

---

### **Преимущества `AuthorizationManager`**

1. **Гибкость:**
   - Позволяет создавать сложную логику авторизации.
   - Можно учитывать как роли пользователя, так и параметры запроса (например, `userId`).

2. **Удобная интеграция:**
   - Подключается напрямую в конфигурации Spring Security через метод `access()`.

3. **Тестируемость:**
   - Код авторизации легко изолировать и протестировать.

---

### Итог

- **`AuthorizationManager`** — это мощный механизм для реализации авторизации в Spring Security.
- Он подключается **после аутентификации**, чтобы решить, разрешен ли доступ.
- Его можно настроить для проверки ролей, параметров запроса и других атрибутов.

Если потребуется пример или уточнение, дайте знать! 😊
