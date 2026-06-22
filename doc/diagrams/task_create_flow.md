```mermaid
flowchart TD

%% =========================
%% FRONTEND
%% =========================

subgraph FRONTEND [Vue Frontend]

A[TaskCreateView.vue]
B[TaskForm.vue]
C[v-model updates form state]
D[submit.prevent]
E[TaskForm.handleSubmit]
F[validate]
G[emit submit]
H[TaskCreateView.handleSubmit]
I[isSubmitting = true]
J[taskService.createTask]
K[apiClient.post]
L[fetch POST /api/tasks]
M[Browser attaches access-token cookie]

A --> B
B --> C
C --> D
D --> E
E --> F
F --> G
G --> H
H --> I
I --> J
J --> K
K --> L
L --> M

end

%% =========================
%% GATEWAY
%% =========================

subgraph GATEWAY [API Gateway]

N[POST /api/tasks]
O[SetPath /api/v1/tasks]
P[lb://task-service]

N --> O
O --> P

end

%% =========================
%% TASK SERVICE SECURITY
%% =========================

subgraph SECURITY [Task Service Security]

Q[Tomcat]
R[FilterChainProxy]
S[SecurityFilterChain]
T[JwtAuthenticationFilter]
U[Extract JWT from Cookie/Header]
V[JwtTokenService.parse]
W[AuthenticatedUser]
X[SecurityContextHolder.setAuthentication]

Q --> R
R --> S
S --> T
T --> U
U --> V
V --> W
W --> X

end

%% =========================
%% CONTROLLER
%% =========================

subgraph CONTROLLER [Controller Layer]

Y[TaskController.createTask]
Z[Jackson JSON to CreateTaskRequest]
AA[Bean Validation @Valid]
AB[@AuthenticationPrincipal AuthenticatedUser]

Y --> Z
Z --> AA
AA --> AB

end

%% =========================
%% USE CASE
%% =========================

subgraph USECASE [Application Layer]

AC[CreateTaskUseCase.create]
AD[CreateTaskUseCaseImpl.validate]

AE[new TaskEntity]
AF[taskId = UUID.randomUUID]
AG[status = NEW]
AH[priority = request or MEDIUM]
AI[createdByUserId = current user]

AC --> AD
AD --> AE
AE --> AF
AE --> AG
AE --> AH
AE --> AI

end

%% =========================
%% PERSISTENCE
%% =========================

subgraph DB [Persistence Layer]

AJ[taskRepository.saveAndFlush]
AK[Hibernate/JPA]
AL[@PrePersist]
AM[createdAt updatedAt]
AN[INSERT INTO tasks]

AJ --> AK
AK --> AL
AL --> AM
AM --> AN

end

%% =========================
%% NOTIFICATION
%% =========================

subgraph NOTIFICATION [Notification Integration]

AO[CreateTaskResponse]
AP[publishAssignmentNotification]

AQ{assigneeUserId exists?}

AR[Skip notification]

AS[TaskNotificationPublisherImpl]
AT[RestNotificationClient]
AU[POST notification-service]

AO --> AP
AP --> AQ

AQ -- No --> AR

AQ -- Yes --> AS
AS --> AT
AT --> AU

end

%% =========================
%% RESPONSE
%% =========================

subgraph RESPONSE [Frontend Response]

AV[HTTP 201 Created]

AW[apiClient.parseResponse]

AX[Extract taskId]

AY[router.push task-details]

AV --> AW
AW --> AX
AX --> AY

end

%% =========================
%% CROSS BLOCK LINKS
%% =========================

M --> N
P --> Q
X --> Y

AB --> AC

AI --> AJ

AN --> AO

AR --> AV
AU --> AV
```
