Глобальный обработчик ошибок в Spring Boot (`@ControllerAdvice`) — это механизм, позволяющий централизованно управлять обработкой исключений, возникающих в приложении. Он помогает стандартизировать ответы на ошибки, улучшить читаемость кода и устранить дублирование обработки ошибок в каждом контроллере.

---

### Основные возможности `@ControllerAdvice`

1. **Централизация обработки исключений:**
   - Логика обработки ошибок вынесена из контроллеров в один класс.

2. **Гибкость:**
   - Обработчик может быть настроен для обработки определенных типов исключений.

3. **Стандартизация:**
   - Все контроллеры возвращают одинаковый формат ошибок.

---

### Как работает `@ControllerAdvice`

1. Когда в контроллере возникает исключение, Spring ищет метод в классе с аннотацией `@ControllerAdvice`, который может обработать это исключение.
2. Если подходящий метод найден, он вызывается вместо стандартной обработки.
3. Ответ формируется на основе возвращаемого значения метода обработчика.

---

### Пример использования

#### 1. Определение класса глобального обработчика ошибок
```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Обработка общего исключения
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred: " + ex.getMessage());
    }
}
```

---

### Основные аннотации и их использование

1. **`@ControllerAdvice`:**
   - Указывает, что класс является обработчиком исключений для всех контроллеров.

2. **`@ExceptionHandler`:**
   - Применяется к методам для обработки определенных типов исключений.
   - Принимает класс исключения как параметр.

   Пример:
   ```java
   @ExceptionHandler(IllegalArgumentException.class)
   public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
       return ResponseEntity
               .status(HttpStatus.BAD_REQUEST)
               .body("Invalid argument: " + ex.getMessage());
   }
   ```

3. **`@ResponseStatus`:**
   - Указывает HTTP-статус, который должен быть возвращен при возникновении исключения.

   Пример:
   ```java
   @ResponseStatus(HttpStatus.NOT_FOUND)
   @ExceptionHandler(ResourceNotFoundException.class)
   public String handleNotFound(ResourceNotFoundException ex) {
       return "Resource not found: " + ex.getMessage();
   }
   ```

4. **`@RestControllerAdvice`:**
   - Это упрощенный вариант `@ControllerAdvice`, который добавляет `@ResponseBody` ко всем методам. Подходит для REST API.

---

### Расширенный пример

#### DTO для стандартного ответа на ошибки
```java
import java.time.Instant;

public class ErrorResponse {
    private Instant timestamp;
    private String message;
    private String details;

    public ErrorResponse(String message, String details) {
        this.timestamp = Instant.now();
        this.message = message;
        this.details = details;
    }

    // Геттеры и сеттеры
}
```

#### Глобальный обработчик ошибок
```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Обработка IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                "Invalid argument",
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Обработка исключений NullPointerException
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(NullPointerException ex) {
        ErrorResponse error = new ErrorResponse(
                "Null pointer exception",
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Обработка всех остальных исключений
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                "Internal server error",
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

---

### Поток обработки исключений

1. Исключение выбрасывается в контроллере.
2. Spring перехватывает исключение и ищет обработчик в классе, помеченном `@ControllerAdvice`.
3. Если обработчик найден:
   - Возвращается результат метода обработчика (например, `ResponseEntity`).
4. Если обработчик не найден:
   - Используется стандартная обработка Spring, возвращающая HTML-страницу с ошибкой (в случае MVC) или JSON-ошибку (в случае REST API).

---

### Пример результата для REST API

Запрос:
```http
GET /api/resource/123
```

Если ресурс не найден и выбрасывается исключение `ResourceNotFoundException`, обработчик может вернуть:
```json
{
    "timestamp": "2024-11-29T12:00:00Z",
    "message": "Resource not found",
    "details": "Resource with ID 123 does not exist"
}
```

---

### Советы по реализации

1. **Создавайте единый стандарт для ошибок:**
   - Определите DTO для ответов на ошибки (например, `ErrorResponse`).
   - Убедитесь, что все обработчики возвращают одинаковую структуру.

2. **Используйте логирование:**
   - Логируйте все исключения для анализа и устранения неисправностей.
   ```java
   private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
   ```

3. **Обрабатывайте часто встречающиеся исключения:**
   - Например, `MethodArgumentNotValidException` для ошибок валидации.

4. **Добавьте кастомные исключения:**
   - Создавайте свои исключения, например `ResourceNotFoundException`, чтобы улучшить читаемость кода.

---

### Пример обработки ошибок валидации

Если вы используете валидацию через `@Valid`, можно обрабатывать ошибки следующим образом:
```java
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
        errors.put(error.getField(), error.getDefaultMessage());
    }
    return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
}
```

---

### Итог

Глобальный обработчик ошибок через `@ControllerAdvice` — мощный инструмент для:
- Централизованной обработки исключений.
- Улучшения удобства сопровождения кода.
- Формирования стандартизированных ответов клиенту.

Глобальный обработчик ошибок (`@ControllerAdvice`) обрабатывает исключения **после** прохождения фильтров (`OncePerRequestFilter`) и интерсепторов (`HandlerInterceptor`). Вот как это работает:

---

### Последовательность обработки в Spring MVC

1. **`OncePerRequestFilter`:**
   - Выполняется на уровне сервлета, до того, как запрос достигает Spring MVC.
   - Перехватывает запросы и может генерировать исключения, например, если токен недействителен.

2. **`HandlerInterceptor`:**
   - Работает на уровне Spring MVC.
   - Выполняется до вызова метода контроллера (метод `preHandle`) и после него (метод `postHandle`).
   - Может выбрасывать исключения, если запрос не соответствует условиям.

3. **Контроллер:**
   - Если запрос успешно прошел фильтры и интерсепторы, он обрабатывается методом контроллера.
   - Если в контроллере возникает исключение, оно передается в глобальный обработчик ошибок.

4. **Глобальный обработчик ошибок (`@ControllerAdvice`):**
   - Перехватывает исключения, выброшенные:
     - В фильтрах (`OncePerRequestFilter`).
     - В интерсепторах (`HandlerInterceptor`).
     - В методах контроллеров.
   - Формирует пользовательский ответ для клиента.

---

### Как это выглядит в потоке выполнения?

1. **Запрос поступает в `DispatcherServlet`.**
2. **Spring MVC запускает цепочку фильтров:**
   - `OncePerRequestFilter` обрабатывает запрос.
   - Если фильтр выбрасывает исключение (например, `InvalidTokenException`), оно передается в глобальный обработчик.
3. **Запускается цепочка интерсепторов:**
   - `HandlerInterceptor.preHandle` выполняет проверки (например, авторизация).
   - Исключения (например, `AccessDeniedException`) передаются в глобальный обработчик.
4. **Запрос передается в контроллер:**
   - Если контроллер выбрасывает исключение (например, `ResourceNotFoundException`), оно также передается в глобальный обработчик.

---

### Пример взаимодействия

1. **`OncePerRequestFilter` выбрасывает исключение:**
   ```java
   if (tokenInvalid) {
       throw new InvalidTokenException("Token is invalid");
   }
   ```

2. **`HandlerInterceptor` выбрасывает исключение:**
   ```java
   if (!userHasPermission) {
       throw new AccessDeniedException("Access denied for user");
   }
   ```

3. **`@ControllerAdvice` обрабатывает их:**
   ```java
   @ExceptionHandler(InvalidTokenException.class)
   public ResponseEntity<String> handleInvalidToken(InvalidTokenException ex) {
       return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
   }

   @ExceptionHandler(AccessDeniedException.class)
   public ResponseEntity<String> handleAccessDenied(AccessDeniedException ex) {
       return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
   }
   ```

---

### Важные моменты

1. **Исключения из фильтров:**
   - Исключения из `OncePerRequestFilter` попадают в `@ControllerAdvice`, если цепочка обработки не была остановлена.
   - Если фильтр напрямую формирует HTTP-ответ (например, `response.sendError()`), обработка на уровне `@ControllerAdvice` не запускается.

2. **Исключения из интерсепторов:**
   - Исключения, выброшенные в `preHandle` или `postHandle` метода `HandlerInterceptor`, обрабатываются глобальным обработчиком, если запрос не был остановлен.

3. **Исключения в `afterCompletion`:**
   - Если в методе `afterCompletion` выбрасывается исключение, оно может быть проигнорировано, так как обработка запроса завершена.

---

### Таблица последовательности

| **Этап**                 | **Обработчик**           | **Исключение перехватывается глобальным обработчиком?** |
|--------------------------|--------------------------|--------------------------------------------------------|
| `OncePerRequestFilter`   | Да, если не вызван `response.sendError()` |
| `HandlerInterceptor.preHandle` | Да                                                     |
| `HandlerInterceptor.postHandle` | Да                                                   |
| `HandlerInterceptor.afterCompletion` | Нет, так как запрос уже завершен               |
| Контроллер               | Да                                                     |

---

### Итог

- Глобальный обработчик ошибок находится **в конце цепочки обработки** и служит резервным механизмом для перехвата исключений, выброшенных на любом уровне (фильтры, интерсепторы, контроллеры).
- Он обрабатывает исключения только в тех случаях, когда запрос не завершен вручную через `response`.

Если нужно больше примеров или пояснений, дайте знать! 😊
