`OncePerRequestFilter` — это абстрактный класс в Spring, используемый для создания фильтров, которые гарантируют, что их логика выполняется **один раз** для каждого HTTP-запроса. Это особенно важно в приложениях Spring Boot, где фильтры могут быть вызваны несколько раз для внутренних перенаправлений или форвардов.

---

### Когда использовать `OncePerRequestFilter`?

- Когда требуется выполнить предобработку или постобработку HTTP-запросов.
- Когда фильтр не должен срабатывать несколько раз при форвардах или включении других ресурсов (например, через `RequestDispatcher`).
- Для логирования, авторизации, обработки CORS, обработки JWT токенов и т.д.

---

### Основные методы

1. **`doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)`**
   - Основной метод, который нужно переопределить.
   - Выполняет вашу логику обработки запроса.
   - После выполнения своей логики вызовите `filterChain.doFilter(request, response)` для передачи запроса следующему фильтру или обработчику.

2. **`shouldNotFilter(HttpServletRequest request)`**
   - Условие, позволяющее исключить определенные запросы из обработки.
   - Если метод возвращает `true`, фильтр пропускается для текущего запроса.

---

### Пример реализации `OncePerRequestFilter`

Создадим фильтр, который проверяет наличие заголовка `Authorization` и валидирует JWT токен.

#### 1. Реализация фильтра
```java
package com.example.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws java.io.IOException, jakarta.servlet.ServletException {

        // Проверяем наличие заголовка Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Если заголовок отсутствует или некорректен, возвращаем 401
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unauthorized: Missing or invalid Authorization header");
            return;
        }

        String jwtToken = authHeader.substring(7); // Убираем "Bearer "
        try {
            // Здесь вы можете валидировать JWT токен
            validateToken(jwtToken);
        } catch (Exception ex) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unauthorized: " + ex.getMessage());
            return;
        }

        // Передаем запрос дальше в цепочке фильтров
        filterChain.doFilter(request, response);
    }

    private void validateToken(String token) throws Exception {
        // Здесь добавьте свою логику проверки JWT токена
        if (token.isEmpty() || !token.equals("valid-token")) {
            throw new Exception("Invalid or expired token");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Исключаем обработку определенных путей (например, логин, регистрация)
        String path = request.getServletPath();
        return path.equals("/api/auth/login") || path.equals("/api/auth/register");
    }
}
```

---

#### 2. Регистрация фильтра в конфигурации Spring Security

В Spring Security 3.3.3 можно зарегистрировать `OncePerRequestFilter` в цепочке фильтров.

```java
package com.example.config;

import com.example.filter.JwtAuthorizationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final JwtAuthorizationFilter jwtAuthorizationFilter;

    public SecurityConfig(JwtAuthorizationFilter jwtAuthorizationFilter) {
        this.jwtAuthorizationFilter = jwtAuthorizationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/auth/**").permitAll() // Открытые эндпоинты
                .anyRequest().authenticated() // Остальные защищены
            )
            .addFilterBefore(jwtAuthorizationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class); // Добавляем наш фильтр
        return http.build();
    }
}
```

---

### Поток выполнения с `OncePerRequestFilter`

1. **Клиент отправляет HTTP-запрос.**
2. Фильтр обрабатывает запрос в методе `doFilterInternal` (если `shouldNotFilter` возвращает `false`).
3. Если логика фильтра завершается успешно:
   - Вызов `filterChain.doFilter(request, response)` передает запрос следующему фильтру или контроллеру.
4. После обработки контроллером, фильтры могут выполнять постобработку (например, логирование).

---

### Реальные примеры использования

1. **JWT Авторизация:**
   - Проверка валидности токена перед вызовом контроллеров.

2. **CORS (Cross-Origin Resource Sharing):**
   - Добавление CORS-заголовков для разрешения междоменных запросов.

3. **Глобальное логирование:**
   - Логирование всех входящих запросов и исходящих ответов.

4. **Глобальная обработка ошибок:**
   - Захват необработанных исключений и возврат кастомизированного ответа клиенту.

---

### Преимущества `OncePerRequestFilter`

- **Гарантированное выполнение один раз за запрос.**
- **Удобная интеграция с Spring Security.**
- **Универсальность:** Можно использовать для любой предобработки или постобработки запросов.



