# API Documentation

## Overview
This document describes the available API endpoints, request/response formats, and how to connect to the Netty socket when a player joins a room.

---

## REST API Endpoints

### Authentication
- **POST /api/auth/login**
  - **Request:**
    ```json
    {
      "username": "string",
      "password": "string"
    }
    ```
  - **Response:**
    ```json
    {
      "token": "string",
      "userId": "string"
    }
    ```

### Room Management
- **POST /api/room/create**
  - **Request:**
    ```json
    {
      "roomName": "string",
      "maxPlayers": 4
    }
    ```
  - **Response:**
    ```json
    {
      "roomId": "string",
      "status": "CREATED"
    }
    ```

- **POST /api/room/join**
  - **Request:**
    ```json
    {
      "roomId": "string",
      "userId": "string"
    }
    ```
  - **Response:**
    ```json
    {
      "roomId": "string",
      "userId": "string",
      "status": "JOINED"
    }
    ```

- **GET /api/room/list**
  - **Response:**
    ```json
    [
      {
        "roomId": "string",
        "roomName": "string",
        "players": ["userId1", "userId2"]
      }
    ]
    ```

---

## WebSocket/Netty Socket Communication

### Connecting to Netty Socket
When a player joins a room, they should connect to the Netty socket server using the provided host and port (see your client configuration).

#### Example (Python):
```python
import socket

HOST = 'your.server.ip'  # Replace with actual server IP
PORT = 12345             # Replace with actual port

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect((HOST, PORT))

# Send authentication or join room message
join_room_msg = '{"action": "join_room", "roomId": "room123", "userId": "user456"}'
sock.sendall(join_room_msg.encode())

# Receive response
response = sock.recv(1024)
print('Received:', response.decode())
```

#### Example (React JavaScript):
```javascript
import React, { useEffect, useRef } from 'react';

const SOCKET_URL = 'ws://localhost:8386';

function RoomSocket() {
  const socketRef = useRef(null);

  useEffect(() => {
    socketRef.current = new WebSocket(SOCKET_URL);

    socketRef.current.onopen = () => {
      // Send join room message when socket opens
      const joinRoomMsg = {
        action: 'join_room',
        roomId: 'room123',
        userId: 'user456'
      };
      socketRef.current.send(JSON.stringify(joinRoomMsg));
    };

    socketRef.current.onmessage = (event) => {
      console.log('Received:', event.data);
    };

    socketRef.current.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    socketRef.current.onclose = () => {
      console.log('WebSocket closed');
    };

    return () => {
      if (socketRef.current) {
        socketRef.current.close();
      }
    };
  }, []);

  return <div>Room Socket Connected</div>;
}

export default RoomSocket;
```

---

## Netty Socket Message Format

### Client to Server
- **Authentication:**
  ```json
  {
    "token": "string",
    "gameId": "string" // Room ID
  }
  ```

- **Room Actions:**
  ```json
  {
    "action": "CREATE|JOIN|LEAVE|CHANGE_HOST",
    "roomId": "string",
    "userId": "string",
    "roomName": "string", // For CREATE
    "maxPlayers": 4, // For CREATE
    "newHostId": "string" // For CHANGE_HOST
  }
  ```

- **Position Sync:**
  ```json
  {
    "action": "sync_position",
    "userId": "string",
    "position": { "x": 0, "y": 0, "z": 0 }
  }
  ```

### Server to Client
- **Authentication Response:**
  ```json
  {
    "status": "SUCCESS|FAILURE",
    "message": "string"
  }
  ```

- **Room Action Response:**
  ```json
  {
    "status": "SUCCESS|FAILURE",
    "message": "string",
    "roomId": "string",
    "actionType": "CREATE|JOIN|LEAVE|CHANGE_HOST"
  }
  ```

- **Room Notifications:**
  ```json
  {
    "type": "PLAYER_JOINED_ROOM|PLAYER_LEFT_ROOM|HOST_CHANGED|ROOM_DELETED",
    "roomId": "string",
    "playerId": "string", // User ID of the player who joined/left/became host
    "playerName": "string", // Username of the player
    "message": "string" // Human-readable message
  }
  ```

---

## Notes
- Always authenticate before joining a room.
- Use the provided token for API requests.
- Socket messages should be in JSON format.
- Refer to `docs/ROOM_API_DOCUMENTATION.md` and `docs/WEBSOCKET_NOTIFICATION_SYSTEM.md` for more details.
