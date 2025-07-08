# Tài liệu API Quản lý Phòng (Room Management API)

## Tổng quan

API Quản lý Phòng cung cấp các chức năng để tạo, quản lý và tham gia vào các phòng chơi game. Hệ thống hỗ trợ 3 loại phòng với các mức độ bảo mật khác nhau.

### Các loại phòng (Room Visibility)

1. **PUBLIC**: Phòng công khai - ai cũng có thể tham gia
2. **LOCKED**: Phòng khóa - yêu cầu mật khẩu để tham gia
3. **HIDDEN**: Phòng ẩn - chỉ có thể tham gia khi được mời

### Base URL
```
${api.prefix}/rooms
```

---

## 1. Tạo phòng mới

### Endpoint
```
POST /rooms
```

### Mô tả
Tạo một phòng mới với các thông tin cơ bản và cài đặt bảo mật.

### Request Body
```json
{
  "name": "Tên phòng",
  "maxPlayers": 4,
  "visibility": "PUBLIC",
  "password": "mật_khẩu_tùy_chọn"
}
```

#### Tham số
| Tham số | Kiểu | Bắt buộc | Mô tả | Giá trị hợp lệ |
|---------|------|----------|-------|----------------|
| name | String | ✅ | Tên phòng | 3-20 ký tự |
| maxPlayers | Integer | ✅ | Số người chơi tối đa | 2-4 |
| visibility | String | ❌ | Loại phòng | PUBLIC, LOCKED, HIDDEN |
| password | String | ❌ | Mật khẩu phòng | Bất kỳ chuỗi nào |

### Response thành công (201 Created)
```json
{
  "room_response_status": 201,
  "message": "Room created successfully",
  "data": {
    "id": "room-uuid-123",
    "name": "Tên phòng",
    "host": {
      "id": "user-123",
      "username": "người_chủ_phòng"
    },
    "players": [
      {
        "id": "user-123",
        "username": "người_chủ_phòng"
      }
    ],
    "maxPlayers": 4,
    "status": "WAITING",
    "visibility": "PUBLIC",
    "gameServerUrl": null
  },
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### Response lỗi (400 Bad Request)
```json
{
  "room_response_status": 400,
  "message": "Room name must be between 3 and 20 characters",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

---

## 2. Lấy danh sách phòng có sẵn

### Endpoint
```
GET /rooms
```

### Mô tả
Lấy danh sách tất cả các phòng đang chờ người chơi. Phòng HIDDEN sẽ không xuất hiện trong danh sách này.

### Request
Không cần request body

### Response thành công (200 OK)
```json
{
  "room_response_status": 200,
  "message": "Available rooms retrieved successfully",
  "data": [
    {
      "id": "room-1",
      "name": "Phòng công khai",
      "host": {
        "id": "user-1",
        "username": "host1"
      },
      "players": [
        {
          "id": "user-1",
          "username": "host1"
        }
      ],
      "maxPlayers": 4,
      "status": "WAITING",
      "visibility": "PUBLIC",
      "gameServerUrl": null
    },
    {
      "id": "room-2",
      "name": "Phòng có mật khẩu",
      "host": {
        "id": "user-2",
        "username": "host2"
      },
      "players": [
        {
          "id": "user-2",
          "username": "host2"
        }
      ],
      "maxPlayers": 2,
      "status": "WAITING",
      "visibility": "LOCKED",
      "gameServerUrl": null
    }
  ],
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

---

## 3. Lấy thông tin chi tiết phòng

### Endpoint
```
GET /rooms/{roomId}
```

### Mô tả
Lấy thông tin chi tiết của một phòng cụ thể.

### Path Parameters
| Tham số | Kiểu | Mô tả |
|---------|------|-------|
| roomId | String | ID của phòng |

### Response thành công (200 OK)
```json
{
  "room_response_status": 200,
  "message": "Room details retrieved successfully",
  "data": {
    "id": "room-123",
    "name": "Phòng chi tiết",
    "host": {
      "id": "user-123",
      "username": "host"
    },
    "players": [
      {
        "id": "user-123",
        "username": "host"
      },
      {
        "id": "user-456",
        "username": "player1"
      }
    ],
    "maxPlayers": 4,
    "status": "WAITING",
    "visibility": "PUBLIC",
    "gameServerUrl": null
  },
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### Response lỗi (404 Not Found)
```json
{
  "room_response_status": 404,
  "message": "Room with ID room-123 not found",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

---

## 4. Tham gia phòng

### Endpoint
```
POST /rooms/{roomId}/join
```

### Mô tả
Tham gia vào một phòng. Yêu cầu mật khẩu tùy thuộc vào loại phòng.

### Path Parameters
| Tham số | Kiểu | Mô tả |
|---------|------|-------|
| roomId | String | ID của phòng |

### Request Body (tùy chọn)
```json
{
  "password": "mật_khẩu_phòng"
}
```

### Response thành công (200 OK)
```json
{
  "room_response_status": 200,
  "message": "Joined room successfully",
  "data": {
    "id": "room-123",
    "name": "Phòng đã tham gia",
    "host": {
      "id": "user-123",
      "username": "host"
    },
    "players": [
      {
        "id": "user-123",
        "username": "host"
      },
      {
        "id": "user-456",
        "username": "player1"
      },
      {
        "id": "current-user-id",
        "username": "current-user"
      }
    ],
    "maxPlayers": 4,
    "status": "WAITING",
    "visibility": "LOCKED",
    "gameServerUrl": null
  },
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### Response lỗi (404 Not Found)
```json
{
  "room_response_status": 404,
  "message": "Room not found",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### Response lỗi (400 Bad Request)
```json
{
  "room_response_status": 400,
  "message": "Room is full",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

```json
{
  "room_response_status": 400,
  "message": "Incorrect room password",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

```json
{
  "room_response_status": 400,
  "message": "This room is hidden and requires invitation to join",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

---

## 5. Mời người chơi vào phòng ẩn

### Endpoint
```
POST /rooms/{roomId}/invite/{userId}
```

### Mô tả
Chủ phòng mời một người dùng vào phòng ẩn. Chỉ áp dụng cho phòng có visibility = HIDDEN.

### Path Parameters
| Tham số | Kiểu | Mô tả |
|---------|------|-------|
| roomId | String | ID của phòng |
| userId | String | ID của người dùng được mời |

### Response thành công (200 OK)
```json
{
  "room_response_status": 200,
  "message": "User invited successfully",
  "data": {
    "id": "room-123",
    "name": "Phòng ẩn",
    "host": {
      "id": "user-123",
      "username": "host"
    },
    "players": [
      {
        "id": "user-123",
        "username": "host"
      },
      {
        "id": "user-456",
        "username": "invited_user"
      }
    ],
    "maxPlayers": 4,
    "status": "WAITING",
    "visibility": "HIDDEN",
    "gameServerUrl": null
  },
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### Response lỗi (400 Bad Request)
```json
{
  "room_response_status": 400,
  "message": "Only the host can invite users",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

```json
{
  "room_response_status": 400,
  "message": "Room is full",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

---

## 6. Rời khỏi phòng

### Endpoint
```
POST /rooms/{roomId}/leave
```

### Mô tả
Rời khỏi phòng hiện tại. Nếu là chủ phòng rời đi, phòng sẽ bị xóa.

### Path Parameters
| Tham số | Kiểu | Mô tả |
|---------|------|-------|
| roomId | String | ID của phòng |

### Response thành công (200 OK)
```json
{
  "room_response_status": 200,
  "message": "Left room successfully",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### Response lỗi (400 Bad Request)
```json
{
  "room_response_status": 400,
  "message": "User is not in this room",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

---

## 7. Bắt đầu trò chơi

### Endpoint
```
POST /rooms/{roomId}/start
```

### Mô tả
Chủ phòng bắt đầu trò chơi. Yêu cầu ít nhất 2 người chơi.

### Path Parameters
| Tham số | Kiểu | Mô tả |
|---------|------|-------|
| roomId | String | ID của phòng |

### Response thành công (200 OK)
```json
{
  "room_response_status": 200,
  "message": "Game started successfully",
  "data": {
    "id": "room-123",
    "name": "Phòng đang chơi",
    "host": {
      "id": "user-123",
      "username": "host"
    },
    "players": [
      {
        "id": "user-123",
        "username": "host"
      },
      {
        "id": "user-456",
        "username": "player1"
      }
    ],
    "maxPlayers": 4,
    "status": "IN_GAME",
    "visibility": "PUBLIC",
    "gameServerUrl": "ws://localhost:8081/game/room-123"
  },
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### Response lỗi (400 Bad Request)
```json
{
  "room_response_status": 400,
  "message": "Only the host can start the game",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

```json
{
  "room_response_status": 400,
  "message": "Need at least 2 players to start the game",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

---

## 8. Thay đổi chủ phòng

### Endpoint
```
POST /rooms/{roomId}/change-host
```

### Mô tả
Chủ phòng hiện tại chuyển quyền chủ phòng cho người chơi khác.

### Path Parameters
| Tham số | Kiểu | Mô tả |
|---------|------|-------|
| roomId | String | ID của phòng |

### Request Body
```json
{
  "newHostId": "user-id-của-chủ-phòng-mới"
}
```

### Response thành công (200 OK)
```json
{
  "room_response_status": 200,
  "message": "Host changed successfully",
  "data": {
    "id": "room-123",
    "name": "Phòng với chủ mới",
    "host": {
      "id": "user-456",
      "username": "new_host"
    },
    "players": [
      {
        "id": "user-123",
        "username": "old_host"
      },
      {
        "id": "user-456",
        "username": "new_host"
      }
    ],
    "maxPlayers": 4,
    "status": "WAITING",
    "visibility": "PUBLIC",
    "gameServerUrl": null
  },
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### Response lỗi (400 Bad Request)
```json
{
  "room_response_status": 400,
  "message": "Only the host can change host permission",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

```json
{
  "room_response_status": 400,
  "message": "New host must be a player in the room",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

---

## Các mã lỗi chung

### 401 Unauthorized
```json
{
  "status": 401,
  "message": "Token validation failed: Token is invalid or expired",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### 403 Forbidden
```json
{
  "status": 403,
  "message": "You do not have permission to access this resource",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

### 500 Internal Server Error
```json
{
  "status": 500,
  "message": "Internal server error occurred",
  "data": null,
  "timestamp": "2024-01-01T12:00:00+07:00"
}
```

---

## Lưu ý quan trọng

1. **Authentication**: Tất cả các endpoint đều yêu cầu JWT token hợp lệ trong header `Authorization: Bearer <token>`

2. **Room Status**: 
   - `WAITING`: Phòng đang chờ người chơi
   - `IN_GAME`: Phòng đang trong trò chơi

3. **Room Visibility**:
   - `PUBLIC`: Ai cũng có thể tham gia
   - `LOCKED`: Cần mật khẩu để tham gia
   - `HIDDEN`: Chỉ có thể tham gia khi được mời

4. **Field Naming**: Các response liên quan đến phòng sử dụng field `room_response_status` thay vì `status`

5. **Game Server URL**: Chỉ được tạo khi trò chơi bắt đầu (status = IN_GAME)

---

## Ví dụ sử dụng

### Tạo phòng công khai
```bash
curl -X POST http://localhost:8080/api/rooms \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Phòng chơi vui",
    "maxPlayers": 4,
    "visibility": "PUBLIC"
  }'
```

### Tạo phòng có mật khẩu
```bash
curl -X POST http://localhost:8080/api/rooms \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Phòng riêng tư",
    "maxPlayers": 2,
    "visibility": "LOCKED",
    "password": "secret123"
  }'
```

### Tham gia phòng có mật khẩu
```bash
curl -X POST http://localhost:8080/api/rooms/room-123/join \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "password": "secret123"
  }'
``` 