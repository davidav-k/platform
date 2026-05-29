### Подробно о **`BearerTokenAuthenticationFilter`** в Spring Security

`BearerTokenAuthenticationFilter` — это фильтр, используемый для обработки **JWT-аутентификации** или других токенов формата Bearer Token. Он является частью **Spring Security OAuth2 Resource Server** и отвечает за аутентификацию пользователей на основе токена, переданного в заголовке HTTP.

---

### **Когда используется `BearerTokenAuthenticationFilter`?**
- Когда вы включаете поддержку OAuth 2.0 Resource Server в Spring Security с помощью конфигурации:
  ```java
  http.oauth2ResourceServer(oauth2 -> oauth2.jwt());
  ```
- Токен передается клиентом в заголовке `Authorization`:
  ```http
  Authorization: Bearer <token>
  ```

---

### **Основная задача фильтра**
`BearerTokenAuthenticationFilter`:
1. Извлекает токен из заголовка `Authorization`.
2. Проверяет его валидность (например, срок действия, подпись).
3. Если токен валиден, создает объект аутентификации (`Authentication`) и сохраняет его в `SecurityContext`.

---

### **Поток выполнения в `BearerTokenAuthenticationFilter`**

1. **Получение HTTP-запроса:**
   - Фильтр обрабатывает каждый входящий HTTP-запрос.
   - Проверяет наличие заголовка `Authorization` с префиксом `Bearer`.

2. **Извлечение токена:**
   - Если заголовок существует, фильтр извлекает токен из строки.
   - Например, из строки:
     ```http
     Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     ```
     Фильтр извлекает токен:
     ```
     eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     ```

3. **Валидность токена:**
   - Токен передается в `JwtDecoder` (или другой компонент для проверки подписи).
   - Если токен недействителен (например, истек срок действия, неверная подпись), выбрасывается исключение (`InvalidBearerTokenException`).

4. **Создание объекта аутентификации:**
   - Если токен валиден, фильтр создает объект `Authentication`:
     ```java
     BearerTokenAuthentication authentication = new BearerTokenAuthentication(
         jwt,
         authorities,
         principal
     );
     ```
   - В объекте `Authentication` содержатся:
     - Данные пользователя (`Principal`).
     - Роли пользователя (`GrantedAuthority`).
     - Дополнительные claims токена.

5. **Сохранение в SecurityContext:**
   - Фильтр сохраняет объект `Authentication` в `SecurityContext`:
     ```java
     SecurityContextHolder.getContext().setAuthentication(authentication);
     ```

6. **Передача запроса дальше:**
   - Если токен валиден, запрос передается следующему компоненту (например, интерсептору или контроллеру).

---

### **Что происходит, если токен недействителен?**
1. Фильтр выбрасывает исключение `AuthenticationException`, такое как:
   - `InvalidBearerTokenException`: токен недействителен.
   - `OAuth2AuthenticationException`: проблемы с токеном OAuth2.
2. Исключение обрабатывается:
   - Обработчиком ошибок Spring Security, например, через:
     ```java
     .authenticationEntryPoint(new CustomBearerTokenAuthenticationEntryPoint())
     ```

---

### **Ключевые методы в `BearerTokenAuthenticationFilter`**

1. **`doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)`**
   - Основной метод, где выполняются шаги:
     - Извлечение токена из заголовка.
     - Аутентификация токена через `AuthenticationManager`.
     - Установка `Authentication` в `SecurityContext`.

2. **`resolveToken(HttpServletRequest request)`**
   - Извлекает токен из заголовка `Authorization`.

3. **`attemptAuthentication(HttpServletRequest request, HttpServletResponse response)`**
   - Проверяет токен и выполняет аутентификацию через `AuthenticationManager`.

4. **`successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult)`**
   - Сохраняет объект `Authentication` в `SecurityContextHolder` при успешной аутентификации.

5. **`unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed)`**
   - Обрабатывает неудачную аутентификацию, отправляя клиенту ошибку (например, `401 Unauthorized`).

---

### **Настройка `BearerTokenAuthenticationFilter` в Spring Security**

#### Конфигурация JWT через `HttpSecurity`:
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(authorize -> authorize
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.decoder(jwtDecoder())) // Подключение JWT-декодера
        )
        .build();
}

@Bean
public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withPublicKey(rsaPublicKey()).build();
}
```

---

### **Как настраивается фильтр в Spring Security?**

1. Spring Security автоматически добавляет `BearerTokenAuthenticationFilter` в цепочку фильтров, если вы включили:
   ```java
   http.oauth2ResourceServer().jwt();
   ```

2. Вы можете настроить обработчики ошибок для фильтра:
   ```java
   http.oauth2ResourceServer(oauth2 -> oauth2
       .jwt(jwt -> jwt.decoder(jwtDecoder()))
       .authenticationEntryPoint(new CustomBearerTokenAuthenticationEntryPoint())
   );
   ```

---

### **Пример потока выполнения**

1. **Запрос от клиента:**
   ```http
   GET /protected-resource HTTP/1.1
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```

2. **`BearerTokenAuthenticationFilter`:**
   - Извлекает токен: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`.
   - Передает токен в `JwtDecoder`.

3. **`JwtDecoder`:**
   - Проверяет подпись, срок действия, claims.
   - Возвращает объект `Jwt`.

4. **`BearerTokenAuthenticationFilter`:**
   - Создает объект `BearerTokenAuthentication`.
   - Устанавливает его в `SecurityContext`.

5. **Контроллер:**
   - Запрос передается в контроллер, где доступен текущий пользователь через `SecurityContext`.

---

### **Итог**

- `BearerTokenAuthenticationFilter` — это мощный компонент для работы с токенами формата Bearer.
- Он извлекает токен из заголовка `Authorization`, валидирует его и создает объект аутентификации.
- При интеграции с Spring Security он автоматически добавляется в цепочку фильтров, обеспечивая прозрачную работу с JWT.

Если нужны примеры кода или более глубокие детали, дайте знать! 😊
