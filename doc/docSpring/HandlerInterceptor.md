`HandlerInterceptor` в Spring Boot — это интерфейс, который позволяет перехватывать запросы на уровне обработки HTTP-запросов перед передачей их контроллерам, после обработки или даже перед отправкой ответа. Это полезный инструмент для выполнения задач, таких как аутентификация, логирование, выполнение предварительной обработки и пост-обработки запросов.

---

### Архитектура `HandlerInterceptor`

`HandlerInterceptor` предоставляет три основных метода, которые могут быть переопределены:

1. **`preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)`**
   - **Когда вызывается:** Перед вызовом метода контроллера.
   - **Цель:**
     - Проверка данных запроса.
     - Аутентификация пользователя.
     - Прекращение обработки и возврат ответа, если определенные условия не выполняются.
   - **Возвращаемое значение:** `true`, чтобы продолжить выполнение цепочки обработки, или `false`, чтобы остановить её.

2. **`postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)`**
   - **Когда вызывается:** После выполнения метода контроллера, но перед отправкой ответа.
   - **Цель:**
     - Изменение данных модели (`ModelAndView`) перед их отправкой.
     - Добавление дополнительной информации в ответ.

3. **`afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)`**
   - **Когда вызывается:** После завершения обработки запроса, независимо от того, произошла ошибка или нет.
   - **Цель:**
     - Очистка ресурсов.
     - Логирование или аналитика обработки запросов.

---

### Пример использования

Создадим `HandlerInterceptor`, который проверяет, передан ли заголовок `Authorization` в запросе.

#### 1. Реализация `HandlerInterceptor`
```java
package com.example.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized: Missing Authorization header");
            return false; // Прекращение обработки запроса
        }
        return true; // Продолжить выполнение цепочки
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // Здесь можно модифицировать данные ответа, если это необходимо
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Очистка ресурсов или логирование
        System.out.println("Request completed for URI: " + request.getRequestURI());
    }
}
```

#### 2. Регистрация `HandlerInterceptor`

Для регистрации интерцептора используйте `WebMvcConfigurer`.

```java
package com.example.config;

import com.example.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**") // Интерцептор будет применяться к этим путям
                .excludePathPatterns("/api/auth/**"); // Исключения для путей
    }
}
```

---

### Поток выполнения запросов с `HandlerInterceptor`

1. Клиент отправляет HTTP-запрос.
2. **`preHandle`**:
   - Проверка запроса (например, заголовки, параметры, авторизация).
   - Возвращает `false`, если запрос должен быть отклонён.
3. Метод контроллера.
4. **`postHandle`**:
   - Обрабатывается после выполнения метода контроллера, перед отправкой ответа.
   - Позволяет модифицировать данные модели.
5. **`afterCompletion`**:
   - Выполняется после завершения обработки запроса (даже если произошла ошибка).
   - Используется для очистки ресурсов, логирования.

---

### Реальные примеры использования

1. **Аутентификация и авторизация:**
   - Проверка токенов или сессий пользователя.
   - Остановка запросов, если пользователь не авторизован.

2. **Логирование:**
   - Запись информации о запросе (URI, IP-адрес, заголовки).
   - Логирование времени выполнения запроса.

3. **Изменение модели:**
   - Добавление глобальных параметров в модель (например, текущий пользователь).

4. **Очистка ресурсов:**
   - Закрытие подключений, например, к базам данных.
   - Удаление временных файлов.

---

### Важные моменты

- **Порядок интерцепторов:** Если зарегистрировано несколько интерцепторов, их вызов будет в порядке добавления в `InterceptorRegistry`.
- **Асинхронные запросы:** Для асинхронных задач нужно убедиться, что интерцепторы не блокируют выполнение.
- **Альтернатива:** В Spring Boot 3.3.3 можно использовать фильтры (`Filter`), если нужно работать с запросами на более низком уровне (например, на уровне Servlet).

Если у вас есть конкретный кейс, для которого нужно настроить интерцептор, напишите, и я помогу его реализовать! 😊
