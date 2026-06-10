# Notification Service API Contract

## Scope

This document describes implemented `notification-service` endpoints and
planned contracts for future work. The service owns notifications,
preferences, templates, and delivery tracking as defined in
[Service boundaries](../architecture/service-boundaries.md).

All external endpoints:

- use the response envelope from [API contract standards](api-contract-standards.md)
- require a valid access JWT
- use UUID strings for notification identifiers and public user references
- return only notifications owned by the authenticated user unless an
  administration contract is added later

## DTOs

### NotificationResponse

| Field | Type | Description |
| --- | --- | --- |
| `notificationId` | UUID string | Notification identifier |
| `recipientUserId` | UUID string | Public identifier of recipient |
| `type` | enum | `SYSTEM` or `EMAIL` |
| `subject` | string or null | Display subject |
| `message` | string | Rendered notification content |
| `read` | boolean | System-notification read state |
| `readAt` | string or null | ISO-8601 timestamp |
| `deliveryStatus` | enum | `PENDING`, `SENT`, `FAILED`, or `NOT_APPLICABLE` |
| `createdAt` | string | ISO-8601 timestamp |

### NotificationPreferenceResponse

| Field | Type | Description |
| --- | --- | --- |
| `userId` | UUID string | Public identifier of preference owner |
| `emailEnabled` | boolean | Whether platform email delivery is enabled |
| `systemEnabled` | boolean | Whether in-application notifications are enabled |
| `taskCreatedEnabled` | boolean | Whether task-created notifications are enabled |
| `taskAssignedEnabled` | boolean | Whether task-assigned notifications are enabled |
| `taskStatusChangedEnabled` | boolean | Whether status notifications are enabled |
| `taskCommentAddedEnabled` | boolean | Whether comment notifications are enabled |
| `updatedAt` | string | ISO-8601 timestamp |

### UpdateNotificationPreferencesRequest

Contains all boolean preference fields from `NotificationPreferenceResponse`.
All fields are required so `PUT` replaces the preference representation.

### SendEmailNotificationRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `requestId` | UUID string | yes | Idempotency identifier |
| `recipientUserId` | UUID string | yes | Existing public user identifier |
| `templateKey` | string | yes | Known internal template identifier |
| `templateParameters` | object | no | Template-specific values; must not contain secrets |

### SendSystemNotificationRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `requestId` | UUID string | yes | Idempotency identifier |
| `recipientUserId` | UUID string | yes | Existing public user identifier |
| `subject` | string | no | Maximum 200 characters |
| `message` | string | yes | Not blank; maximum 5000 characters |
| `sourceType` | string | yes | Producing domain, for example `TASK` |
| `sourceId` | UUID string | no | Related aggregate identifier |

## External Notification Endpoints

### `GET /api/v1/notifications`

Returns the authenticated user's notifications.

- Request DTO: none
- Response: `200 OK`, `data.items` contains `NotificationResponse` entries and
  `data.page` contains standard pagination metadata
- Authorization: authenticated owner only
- Pagination: standard `page`, `size`, and `sort`; default sort is
  `createdAt,desc`
- Optional filters: `read`, `type`, `deliveryStatus`
- Validation: enum filters must be supported values

### `GET /api/v1/notifications/{notificationId}`

Returns one notification owned by the authenticated user.

- Request DTO: none
- Response: `200 OK`, `data.notification` contains `NotificationResponse`
- Authorization: authenticated owner only
- Validation: `notificationId` must be a UUID

### `PATCH /api/v1/notifications/{notificationId}/read`

Marks an owned system notification as read. Repeating the operation is
idempotent.

- Request DTO: none
- Response: `200 OK`, `data.notification` contains `NotificationResponse`
- Authorization: authenticated owner only
- Validation: `notificationId` must be a UUID

## External Preference Endpoints

### `GET /api/v1/notification-preferences`

Returns the authenticated user's notification preferences. Defaults are
returned when no stored override exists.

- Request DTO: none
- Response: `200 OK`, `data.preferences` contains
  `NotificationPreferenceResponse`
- Authorization: authenticated owner only

### `PUT /api/v1/notification-preferences`

Replaces the authenticated user's notification preferences.

- Request DTO: `UpdateNotificationPreferencesRequest`
- Response: `200 OK`, `data.preferences` contains
  `NotificationPreferenceResponse`
- Authorization: authenticated owner only
- Validation: all preference fields are required booleans

## Internal Delivery Endpoints

Internal endpoints are for service-to-service calls only. API Gateway must not
route them. The implemented system endpoint uses a delegated platform access
JWT for the MVP; dedicated service authentication and idempotency remain future
work. Internal calls require audit-safe logging.

### `POST /internal/api/v1/notifications/email`

Requests email notification delivery.

- Request DTO: `SendEmailNotificationRequest`
- Response: `202 ACCEPTED`, `data.notification` contains
  `NotificationResponse`
- Caller: trusted platform service only

### `POST /internal/api/v1/notifications/system`

Creates an in-application notification.

- Request DTO: `CreateSystemNotificationRequest`
- Response: `201 CREATED`, `data.notification` contains
  `NotificationResponse`
- Caller: trusted platform service only
- Authentication: valid platform access JWT; task-service delegates the initiating user's JWT for the MVP
- Required fields: `recipientUserId`, `type`, `message`, `sourceService`, `sourceEntityType`, `sourceEntityId`
- Optional field: `title`
- The endpoint always creates an `IN_APP` notification with `PENDING` status

## Delivery Notes

- General platform email delivery belongs to `notification-service`.
- Existing `user-service` account-verification email remains in
  `user-service` during MVP stabilization.
- Push and WebSocket delivery are outside this initial contract.
- Future event consumers may replace internal REST calls without changing
  external notification endpoints.
