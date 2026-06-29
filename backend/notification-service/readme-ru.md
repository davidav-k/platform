# Notification Service

Notification Service отвечает за хранение и чтение уведомлений платформы.
Task notifications создаются через Kafka consumer, который читает события из
Outbox Pattern task-service.

## Статус

Сервис интегрирован в Docker Compose на порту `8087` и доступен через API Gateway
по `/api/notifications`. Runtime configuration приходит из
`config/notification-service-dev.yml`, схема базы управляется Flyway.

## Доменная модель

`NotificationEntity` хранит:

- `notificationId`
- `recipientUserId`
- `type`
- `channel`
- `subject`
- `body`
- `status`
- `sourceService`
- `sourceEntityType`
- `sourceEntityId`
- timestamps и failure metadata

Текущие enum значения:

- `NotificationType`: `TASK_ASSIGNED`, `TASK_CREATED`, `SYSTEM`
- `NotificationChannel`: `EMAIL`, `IN_APP`
- `NotificationStatus`: `PENDING`, `SENT`, `FAILED`

## API

Public API:

- `POST /api/v1/notifications` - создаёт notification row. Email не отправляется.
- `GET /api/v1/notifications/{notificationId}` - читает одно уведомление.
- `GET /api/v1/notifications` - список уведомлений с фильтрами
  `recipientUserId`, `status`, `channel`, `type`, `page`, `size`, `sort`.

Internal API:

- `POST /internal/api/v1/notifications/system` - внутренний endpoint для system
  notifications. API Gateway его не публикует наружу.

Task-service не использует internal REST endpoint для task notifications.

## Kafka Task Event Processing

Единственный текущий механизм доставки task notifications:

```text
task-service
  -> outbox_events
  -> Kafka topic platform.task-events
  -> NotificationEventConsumer
  -> TaskEventNotificationProcessor
  -> CreateSystemNotificationUseCase
  -> notifications
```

Consumer включается настройкой `notification.kafka.enabled=true`.

Поддерживаемые события:

| Event type | Notification behavior |
| --- | --- |
| `TASK_CREATED` | создаёт `IN_APP` notification type `TASK_CREATED`, если `assigneeUserId != null` |
| `TASK_ASSIGNED` | создаёт `IN_APP` notification type `TASK_ASSIGNED`, если `newAssigneeUserId != null` |
| `TASK_STATUS_CHANGED` | создаёт `IN_APP` notification type `SYSTEM`, если `assigneeUserId != null` |

Payload fields, ожидаемые от task-service:

- `taskId`
- `title`
- `description` для `TASK_CREATED`
- `status`
- `priority`
- `assigneeUserId` для `TASK_CREATED` / `TASK_STATUS_CHANGED`
- `previousAssigneeUserId` и `newAssigneeUserId` для `TASK_ASSIGNED`
- `previousStatus` и `newStatus` для `TASK_STATUS_CHANGED`
- `createdByUserId`
- `createdAt` или `updatedAt`

## Идемпотентность

`event_consumption_log` защищает consumer от повторной обработки одного Kafka
event. Таблица хранит:

- `event_id`
- `event_type`
- `consumed_at`
- `source`

Если `event_id` уже присутствует в журнале, notification повторно не создаётся.

## Локальный запуск

```bash
docker compose --env-file .env -f compose.yml up -d --build notification-service
```

Health check:

```text
GET http://localhost:8087/actuator/health
```

Gateway route:

```text
http://localhost:8080/api/notifications
```

Direct service route:

```text
http://localhost:8087/api/v1/notifications
```

## Аутентификация

Public notification API требует JWT:

- `Authorization: Bearer <token>`
- cookie `access-token`

`/actuator/health` и `/actuator/info` остаются публичными.

## Тесты

```bash
mvn -B -f backend/notification-service/pom.xml test
```

## Не реализовано

- SMTP/email delivery;
- preferences API;
- read state / mark-as-read;
- delete notification endpoint;
- status transition API;
- RBAC/ownership filtering внутри notification-service.
