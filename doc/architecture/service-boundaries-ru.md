# Границы сервисов

## Текущая фаза

MVP запускает `user-service`, `task-service`, `notification-service`, API
Gateway, Vue frontend, Config Server, Eureka, PostgreSQL, Redis, Kafka,
MailHog и Zipkin в Docker Compose.

Для уведомлений о созданных задачах используется только event-driven путь:

```text
task-service -> outbox_events -> Kafka platform.task-events
  -> notification-service -> notifications
```

HTTP-трафик frontend остаётся синхронным и проходит через API Gateway. Kafka и
outbox сейчас используются для доставки task notification events, а не как
общая замена всех межсервисных вызовов.

## Владение доменами

| Домен | Владелец | Статус |
| --- | --- | --- |
| Users, roles, auth, MFA, profile, account lifecycle | `user-service` | Реализовано |
| Tasks, assignee, status, soft delete, task outbox events | `task-service` | Реализовано |
| Notifications, notification preferences persistence, consumed events | `notification-service` | Реализовано частично |

Task comments, task history, notification templates, delivery attempts,
notification preferences API и mark-as-read API сейчас не реализованы.

## Разрешённые зависимости

| Caller | Callee | Назначение |
| --- | --- | --- |
| Frontend | API Gateway | Только публичные `/api/**` маршруты |
| API Gateway | routed services | Маршрутизация и ранняя JWT-проверка |
| `task-service` | Kafka | Публикация событий из `outbox_events` |
| Kafka | `notification-service` | Доставка task events consumer-у |

Сервисы не читают и не пишут таблицы других сервисов. Каждый сервис сам
валидирует JWT и принимает решения по доступу к собственным данным.

## Запрещённые зависимости

- `task-service` не обращается к базе `user-service`.
- `notification-service` не обращается к базе `task-service`.
- `task-service` не использует старый прямой REST-путь в
  `notification-service` для `TASK_CREATED`.
- Сервисы не используют общие entity или общие persistence-модели.
- Gateway headers не являются единственным доказательством аутентификации.

## Gateway routes

| Внешний маршрут | Внутренний маршрут | Сервис | Статус |
| --- | --- | --- | --- |
| `/api/users/**` | `/api/v1/user/**` | `user-service` | Реализовано |
| `/api/tasks` | `/api/v1/tasks` | `task-service` | Реализовано |
| `/api/tasks/**` | `/api/v1/tasks/**` | `task-service` | Реализовано |
| `/api/notifications` | `/api/v1/notifications` | `notification-service` | Реализовано |
| `/api/notifications/**` | `/api/v1/notifications/**` | `notification-service` | Реализовано |

Gateway не маршрутизирует `/internal/api/v1/notifications/system`.
