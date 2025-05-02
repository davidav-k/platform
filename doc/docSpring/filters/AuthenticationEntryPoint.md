### **AuthenticationEntryPoint в Spring Boot 3.3.3**

**`AuthenticationEntryPoint`** — это интерфейс Spring Security, который отвечает за обработку ошибок **аутентификации**. Он используется для формирования ответа клиенту в случае, если запрос не прошел аутентификацию. Чаще всего возвращается код HTTP `401 Unauthorized`.

---

### **Когда используется `AuthenticationEntryPoint`?**

`AuthenticationEntryPoint` вызывается в следующих ситуациях:
1. Когда пользователь пытается получить доступ к защищенному ресурсу без предоставления учетных данных.
2. Когда предоставленные учетные данные (например, токен или логин/пароль) недействительны.
3. Когда механизм аутентификации (например, Basic Auth или JWT) не смог идентифицировать пользователя.

---

### **Где находится в потоке обработки?**

1. **На уровне фильтров Spring Security:**
   - Если фильтры аутентификации (например, `BasicAuthenticationFilter` или `BearerTokenAuthenticationFilter`) обнаруживают, что пользователь не прошел аутентификацию, они вызывают `AuthenticationEntryPoint`.

2. **До вызова контроллера:**
   - `AuthenticationEntryPoint` срабатывает еще до того, как запрос достигает `DispatcherServlet`.

---

### **Основные методы**

#### **1. Метод `commence`:**
Это единственный метод интерфейса `AuthenticationEntryPoint`. Он вызывается, когда требуется ответить на ошибку аутентификации.

```java
void commence(HttpServletRequest request,
              HttpServletResponse response,
              AuthenticationException authException)
              throws IOException, ServletException;
```

- **Аргументы:**
  - `HttpServletRequest`: объект запроса, в котором содержатся все данные клиента.
  - `HttpServletResponse`: объект ответа, который формируется для клиента.
  - `AuthenticationException`: исключение, описывающее причину ошибки аутентификации (например, недействительный токен, отсутствующий заголовок `Authorization`).

---

### **Пример реализации `AuthenticationEntryPoint`**

#### **Код кастомного `AuthenticationEntryPoint`:**
```java
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Устанавливаем HTTP-статус 401 Unauthorized
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        
        // Формируем тело ответа
        String json = String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", authException.getMessage());
        response.getWriter().write(json);
    }
}
```

#### **Пример ответа:**
```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

---

### **Подключение в Spring Security**

#### **Через конфигурацию:**
Чтобы использовать кастомный `AuthenticationEntryPoint`, нужно указать его в настройках Spring Security.

```java
@Configuration
public class SecurityConfig {
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    public SecurityConfig(CustomAuthenticationEntryPoint customAuthenticationEntryPoint) {
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(customAuthenticationEntryPoint) // Указываем кастомный обработчик
            )
            .httpBasic(Customizer.withDefaults()) // Basic Authentication
            .csrf(csrf -> csrf.disable())
            .build();
    }
}
```

---

### **Пример потока выполнения**

#### **HTTP-запрос:**
```http
GET /protected-resource HTTP/1.1
Host: example.com
```

#### **Сценарий:**

1. Клиент отправляет запрос к защищенному ресурсу без заголовка `Authorization`.
2. Фильтр Spring Security обнаруживает отсутствие аутентификации и выбрасывает `AuthenticationException`.
3. `AuthenticationEntryPoint` перехватывает это исключение и формирует HTTP-ответ:
   ```http
   HTTP/1.1 401 Unauthorized
   Content-Type: application/json

   {
       "error": "Unauthorized",
       "message": "Full authentication is required to access this resource"
   }
   ```

---

### **Расширенные сценарии использования**

1. **Кастомизация ответа:**
   - Можно возвращать не только JSON, но и HTML или другой формат, в зависимости от требований клиента.

2. **Логирование:**
   - Можно добавить логи, чтобы отслеживать все случаи неудачной аутентификации:
     ```java
     log.warn("Unauthorized access attempt: {}", request.getRequestURI());
     ```

3. **Мультиплатформенность:**
   - Вы можете проверять заголовки запроса, чтобы адаптировать ответ для различных клиентов (например, браузеров, мобильных приложений).

---

### **Итог**

`AuthenticationEntryPoint` — это ключевой компонент для обработки ошибок аутентификации в Spring Security. Он:
- Формирует ответ клиенту, когда запрос не прошел аутентификацию.
- Гибко настраивается под различные сценарии.
- Работает в связке с фильтрами Spring Security, такими как `BasicAuthenticationFilter` и `BearerTokenAuthenticationFilter`.

Если нужно углубиться в определенные аспекты, например, в настройку или интеграцию с `AccessDeniedHandler`, дайте знать! 😊