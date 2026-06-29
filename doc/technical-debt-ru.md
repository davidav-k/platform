# Технический долг

| Проблема | Влияние | Приоритет | План | Статус |
| --- | --- | --- | --- | --- |
| Нет root Maven aggregator POM | Maven modules запускаются отдельно, CI coverage нужно поддерживать явно | Low | Рассмотреть aggregator POM после стабилизации build layout | Open |
| Testcontainers 1.x требует Docker API compatibility override с Docker Engine 29 | Backend integration tests зависят от `api.version=1.44` в Maven Surefire | Medium | Обновить Testcontainers, когда dependency baseline будет совместим, затем убрать override | Open |
| GitHub secret scanning и push protection не подтверждены | Repository protections могут быть отключены на GitHub | High | Администратор репозитория должен проверить настройки GitHub | Partially completed |
| Branch protection rules не подтверждены | Возможны direct pushes или merge без CI/review | Medium | Настроить правила для `main` и `dev` | Partially completed |
| User-service hard delete не соответствует MVP deactivation policy | Можно потерять identity source, пока task/notification сохраняют user references | High | Согласовать lifecycle semantics с `doc/architecture/user-deletion-policy.md` | Policy defined; implementation open |
| task-service MVP scope: нет comment endpoints | Task lifecycle остаётся неполным | Medium | Реализовать comment endpoints отдельным шагом | Open |
| task-service authorization для planned comment operations ещё не определён | Реализованные task операции уже имеют role/ownership rules, но comments ещё нет | Medium | Добавить правила при реализации comments | Partially mitigated |
| task assignment не проверяет существование пользователя | Задача может ссылаться на UUID несуществующего или неактивного пользователя | Medium | Добавить service-to-service validation или user lifecycle events | Open |
