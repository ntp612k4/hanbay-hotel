# 12. Coding Convention

# HANBAY HOTEL
## Coding Convention

---

# 1. Backend Package Structure

```text
com.hanbay.hotel
├── auth
├── user
├── room
├── booking
├── payment
├── pricing
├── housekeeping
├── ai
└── common
```

Each module:

```text
controller
service
repository
entity
dto
mapper
exception
```

---

# 2. Naming Convention

## Class

```text
BookingService
BookingController
BookingRepository
BookingRequest
BookingResponse
```

## Method

```text
createBooking()
cancelBooking()
calculateAvailability()
```

## Variable

```text
checkInDate
checkOutDate
roomTypeId
```

---

# 3. REST API Convention

Use plural resources:

```text
/bookings
/room-types
```

Use HTTP methods correctly:

```text
GET    read
POST   create
PUT    replace
PATCH  partial update/action
DELETE delete
```

Mỗi API phải được mô tả bằng một mức bảo mật: `PUBLIC`, `AUTHENTICATED` hoặc `ROLE_RESTRICTED`. Với `ROLE_RESTRICTED`, phải liệt kê rõ các vai trò được phép. API được bảo vệ phải mô tả lỗi `401 AUTH_UNAUTHORIZED`; API giới hạn theo vai trò phải mô tả thêm lỗi `403 AUTH_FORBIDDEN`. Khi yêu cầu chưa rõ, ghi `CẦN XÁC NHẬN NGHIỆP VỤ` thay vì tự suy đoán vai trò được phép.

---

# 4. Response Format

```json
{
  "success": true,
  "message": "OK",
  "data": {},
  "errors": null
}
```

---

# 5. Exception Handling

Use global exception handler:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

Recommended exceptions:

- BusinessException
- ResourceNotFoundException
- UnauthorizedException
- ConflictException
- ValidationException

---

# 6. Validation

Use Bean Validation:

```java
@NotNull
@NotBlank
@Email
@Future
```

---

# 7. DTO Rule

Do not expose Entity directly to API.

Use:

```text
Request DTO
Response DTO
Mapper
```

---

# 8. Git Commit Convention

```text
feat: add booking API
fix: prevent overbooking
docs: update API spec
refactor: clean booking service
test: add booking unit test
chore: update docker config
```

---

# 9. Code Review Checklist

- Code compiles
- No unused imports
- API has validation
- Business rule handled
- Exception handled
- Swagger updated
- Tests added when needed
