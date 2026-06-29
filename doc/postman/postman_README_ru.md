# Postman MVP End-to-End Checks

1. Готовность API Gateway.
2. Авторизацию администратора.
3. Регистрацию и активацию обычного пользователя.
4. Login обычного пользователя.
5. Создание задачи обычным пользователем.
6. Получение, обновление, изменение статуса и soft delete задачи.
7. Проверку ownership/RBAC.
8. Создание задачи администратором с назначением обычного пользователя.
9. Проверку Kafka/outbox notification flow для `TASK_CREATED` / `IN_APP`
10. Refresh token flow.
11. Текущий logout-контракт


## Переменные окружения Postman

| Variable | Назначение |
|---|---|
| `baseUrl` | URL API Gateway. По умолчанию `http://localhost:8080`. |
| `adminEmail` | Email администратора. По умолчанию `admin@mail.com`. |
| `adminPassword` | Пароль администратора из `.env` / `ADMIN_PASSWORD`. Заполняется вручную. |
| `userEmail` | Email тестового обычного пользователя. |
| `userPassword` | Пароль тестового обычного пользователя. Заполняется вручную. |
| `accessToken` | Текущий access token. Заполняется автоматически. |
| `refreshToken` | Текущий refresh token. Заполняется автоматически. |
| `adminAccessToken` | Access token администратора. Заполняется автоматически. |
| `userAccessToken` | Access token обычного пользователя. Заполняется автоматически. |
| `adminUserId` | UUID администратора. Заполняется автоматически. |
| `userId` | UUID обычного пользователя. Заполняется автоматически. |
| `taskId` | UUID основной тестовой задачи. Заполняется автоматически. |
| `assignedTaskId` | UUID задачи, назначенной обычному пользователю. Заполняется автоматически. |
| `unrelatedTaskId` | UUID задачи администратора для RBAC/ownership checks. Заполняется автоматически. |
| `notificationId` | UUID найденного уведомления. Заполняется автоматически. |
| `notificationWaitMillis` | Пауза ожидания Kafka/outbox обработки. Рекомендуется `7000`. |

Проверка уведомлений создаёт задачу с `assigneeUserId`, ждёт
`notificationWaitMillis`, затем читает `GET /api/notifications` через Gateway
и ожидает `TASK_CREATED` notification для назначенного пользователя. Пауза
нужна, потому что task-service сначала пишет outbox event, а
notification-service создаёт notification асинхронно после доставки через
Kafka.


## Как запускать
1. Импорт collection
2. Импорт environment
3. environment `Platform Local - Postman Template`.
4. `adminPassword` и `userPassword`.
