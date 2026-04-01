# Task Tracker (Reactive MongoDB + Security) 

## Описание (РЕАЛЬНЫЙ код)

**Reactive WebFlux** + **MongoDB** + **Spring Security** с **MapStruct**.

**Пути**: `/users`, `/task` (НЕ `/tasks`).

**Пользователи**: `manager:password`, `user:password`.

---

## Безопасность (SecurityConfig)

**In-Memory пользователи**:
- manager:password → ROLE_MANAGER
- user:password → ROLE_USER

**Права**:
| Эндпоинт | Метод | Роли |
|----------|-------|------|
| `/users/**` | **ВСЕ** | **USER/MANAGER** |
| `/task` | **GET** | **USER/MANAGER** |
| `/task` | **POST/PUT/PATCH/DELETE** | **MANAGER** |

**HTTP Basic** + **@PreAuthorize**.

---

## Эндпоинты

### Users `/users`
```http
GET    /users                    # Flux<UserResponse>
GET    /users/{id}               # Mono<UserResponse> 
POST   /users                    # Mono<UserResponse>
PUT    /users/{id}               # Mono<UserResponse>
DELETE /users/{id}               # Mono<Void>
```

### Tasks `/task`
```http
GET    /task                     # Flux<TaskResponse> (с relations)
GET    /task/{id}                # Mono<TaskResponse> (с relations)
POST   /task                     # Mono<TaskResponse>
PUT    /task/{id}                # Mono<TaskResponse>
PATCH  /task/{id}/observers/{observerId}  # Mono<TaskResponse>
DELETE /task/{id}                # Mono<Void>
```

**TaskResponse** содержит `author`, `assignee`, `observers`.

---

## Запуск & Тестирование (Postman)

```bash
docker-compose up -d
```

### 1. Создать пользователей
```bash
# MANAGER
curl -X POST http://localhost:8080/users \
  -u manager:password \
  -H "Content-Type: application/json" \
  -d '{
    "username": "mgr1",
    "email": "mgr@test.com", 
    "roles": ["ROLE_MANAGER"]
  }'

# USER
curl -X POST http://localhost:8080/users \
  -u manager:password \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1", 
    "email": "user@test.com",
    "roles": ["ROLE_USER"]
  }'
```

### 2. Создать задачу (MANAGER)
```bash
curl -X POST http://localhost:8080/task \
  -u manager:password \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Fix login bug",
    "description": "Critical security issue",
    "status": "TODO",
    "authorId": "mgr1-id",
    "assigneeId": "user1-id"
  }'
```

**Ответ** (с relations):
```json
{
  "id": "task123",
  "name": "Fix login bug",
  "author": {
    "id": "mgr1-id",
    "username": "mgr1"
  },
  "assignee": {
    "id": "user1-id", 
    "username": "user1"
  },
  "observers": []
}
```

### 3. Добавить наблюдателя (MANAGER)
```bash
curl -X PATCH http://localhost:8080/task/task123/observers/user1-id \
  -u manager:password
```

### 4. Получить все задачи (USER/MANAGER)
```bash
curl http://localhost:8080/task -u user:password
```

### 5. Тест 403 (USER не может создать задачу)
```bash
curl -X POST http://localhost:8080/task \
  -u user:password \
  -d {...}  # → 403 Forbidden
```

---

## Ключевые моменты

### 1. TaskService.populateTaskRelations()
```java
Mono.zip(authorMono, assigneeMono, observerFlux)
    .map(tuple → TaskResponse.builder()...)
```

**Mono.zip** объединяет User'ов для `author/assignee/observers`.

### 2. @ReadOnlyProperty
author, assignee, observers НЕ попадают в MongoDB

### 3. MapStruct
TaskMapper.taskToResponse(Task) → TaskResponse (с вложенными User)

### 4. ReactiveMongoRepository
Flux/Mono из коробки

---