Platform is a Spring Boot microservices project.

When generating code:

- Follow existing project style.
- Keep changes minimal.
- Do not rewrite unrelated code.
- Do not introduce new frameworks.
- Do not add dependencies without justification.
- Keep controllers thin.
- Keep business logic in services.
- Use DTOs for API contracts.
- Add Javadoc to new classes.
- Prefer constructor injection.
- Prefer explicit code over clever code.

For every task:

1. Analyze current implementation.
2. Explain issue.
3. Implement smallest possible solution.
4. List changed files.
5. Suggest tests.

Security-sensitive areas:

- JWT
- Cookies
- Authentication
- Authorization
- Gateway filters

Verify security implications before making changes.
