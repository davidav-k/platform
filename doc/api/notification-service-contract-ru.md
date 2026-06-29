# Контракт API notification-service

## Область

Документ описывает реализованные HTTP endpoint-ы `notification-service` и
Kafka-backed поток task notifications. Сервис владеет таблицами уведомлений и
идемпотентностью обработки событий. Он не владеет задачами или профилями
пользователей.

Все внешние endpoint-ы:

- используют общий response envelope;
- требуют валидный access JWT;
- используют UUID для notification identifiers и user references.

Фильтрация по владельцу на основе authenticated user и role-based
authorization для notification API пока не реализованы.

## DTO

### NotificationResponse

| Поле | Тип |
| --- | --- |
| `notificationId` | UUID |
| `recipientUserId` | UUID |
| `type` | `TASK_ASSIGNED`, `TASK_CREATED`, `SYSTEM` |
| `channel` | `EMAIL`, `IN_APP` |
| `subject` | string или null |
| `body` | string |
| `status` | `PENDING`, `SENT`, `FAILED` |
| `createdAt` | timestamp |
| `updatedAt` | timestamp |
| `sentAt` | timestamp или null |
| `failureReason` | string или null |

### CreateNotificationRequest

| Поле | Обязательное | Правило |
| --- | --- | --- |
| `recipientUserId` | да | UUID |
| `type` | да | `TASK_ASSIGNED`, `TASK_CREATED`, `SYSTEM` |
| `channel` | да | `EMAIL`, `IN_APP` |
| `subject` | нет | max 255 |
| `body` | да | not blank, max 5000 |

### CreateSystemNotificationRequest

| Поле | Обязательное | Правило |
| --- | --- | --- |
| `recipientUserId` | да | UUID |
| `type` | да | `TASK_ASSIGNED`, `TASK_CREATED`, `SYSTEM` |
| `title` | нет | max 255 |
| `message` | да | not blank, max 5000 |
| `sourceService` | да | not blank, max 100 |
| `sourceEntityType` | да | not blank, max 100 |
| `sourceEntityId` | да | UUID |

System notification use case всегда сохраняет `channel=IN_APP` и
`status=PENDING`.

## Внешние endpoint-ы

### `POST /api/v1/notifications`

Gateway route: `POST /api/notifications`.

Создаёт запись уведомления. Email delivery не выполняется.

### `GET /api/v1/notifications/{notificationId}`

Gateway route: `GET /api/notifications/{notificationId}`.

Возвращает одно уведомление по UUID.

### `GET /api/v1/notifications`

Gateway route: `GET /api/notifications`.

Возвращает список уведомлений с фильтрами:

- `recipientUserId`
- `status`
- `channel`
- `type`
- `page`
- `size`
- `sort`, по умолчанию `createdAt,desc`

## Internal endpoint

### `POST /internal/api/v1/notifications/system`

Создаёт `IN_APP` system notification. API Gateway этот endpoint не
маршрутизирует. `task-service` больше не использует его для `TASK_CREATED`;
такие уведомления создаются через Kafka consumer.

## Kafka task notifications

Поддерживаемый поток:

```text
task-service -> outbox_events -> Kafka platform.task-events
  -> notification-service -> notifications
```

`NotificationEventConsumer` включается через
`notification.kafka.enabled=true`, читает `notification.kafka.topic` и пишет
обработанные события в `event_consumption_log`. Уникальный `event_id` является
idempotency key.

`TaskEventNotificationProcessor` обрабатывает:

| Event type | Результат |
| --- | --- |
| `TASK_CREATED` | `IN_APP` `TASK_CREATED` для `assigneeUserId`, если он есть |
| `TASK_ASSIGNED` | `IN_APP` `TASK_ASSIGNED` для `newAssigneeUserId`, если он есть |
| `TASK_STATUS_CHANGED` | `IN_APP` `SYSTEM` для `assigneeUserId`, если он есть |

События без нужного recipient user ID считаются обработанными, но notification
row не создаётся.

## Не реализовано

- Notification preferences HTTP API
- Mark-as-read / read state
- Delete notification endpoint
- Notification status update endpoint
- SMTP/email delivery из notification-service
- WebSocket, push или realtime delivery
